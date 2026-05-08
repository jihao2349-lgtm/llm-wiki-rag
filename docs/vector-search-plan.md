# Wiki 向量化与混合检索 — 实施方案

> 版本：v1.0
> 创建日期：2026-05-08
> 触发方式：**手动触发**（不自动跑在摄入流水线中）
> 检索策略：向量相似度 + 关键词评分 → RRF 融合

---

## 一、目标与范围

### 1.1 业务目标

在现有「关键词分词检索」基础上，叠加**向量语义检索**，使 RAG 召回率提升：
- 用户问"数据库连不上" → 能召回标题为"连接池配置"的 Wiki 页面
- 用户问"系统跑得慢" → 能召回标题为"性能优化清单"的 Wiki 页面

### 1.2 非目标

- ❌ 不替换现有关键词检索（关键词检索作为 fallback 与混合检索的另一路）
- ❌ 不引入独立向量数据库（用 MySQL 8.4 原生 VECTOR）
- ❌ 不在摄入流水线中自动触发向量化（避免 API 成本失控、用户感知滞后）

### 1.3 范围

| 模块 | 改动类型 |
|------|---------|
| 数据库 schema | 新增 |
| 后端：embedding domain | 新增 |
| 后端：search 混合检索 | 新增 |
| 后端：settings 扩展 | 修改 |
| 后端：API 接口 | 新增 |
| 前端：向量管理页 | 新增 |
| 前端：摄入完成通知 | 修改 |
| 前端：设置页 | 修改 |

---

## 二、技术选型

| 项 | 选型 | 备注 |
|----|------|------|
| 向量库 | **MySQL 8.4 VECTOR(1024) + HNSW 索引** | 服务器已有，零基础设施成本 |
| Embedding 服务 | **OpenAI 兼容协议 `/v1/embeddings`** | 默认对接 DashScope `text-embedding-v3` |
| 向量维度 | **1024** | DashScope v3 默认输出 |
| 触发方式 | **手动**（按钮 + 通知） | 摄入完成后提醒，不自动跑 |
| 融合算法 | **RRF（Reciprocal Rank Fusion，k=60）** | 工业界主流，无需调权重 |

---

## 三、数据库设计

### 3.1 Flyway 迁移文件

**文件：** `backend/src/main/resources/db/migration/V002__add_wiki_embedding.sql`

```sql
-- 为 wiki_page 表添加向量字段
ALTER TABLE wiki_page
  ADD COLUMN embedding VECTOR(1024) COMMENT '页面摘要向量',
  ADD COLUMN embedding_model VARCHAR(64) COMMENT '生成向量所用模型',
  ADD COLUMN embedded_at DATETIME COMMENT '向量生成时间',
  ADD COLUMN embed_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
  ADD COLUMN embed_content_hash VARCHAR(64) COMMENT '向量化时内容的 sha256 hash，用于增量判断',
  ADD COLUMN embed_error VARCHAR(500) COMMENT '失败原因';

-- HNSW 向量索引（MySQL 8.4 原生支持）
CREATE VECTOR INDEX idx_wiki_embedding ON wiki_page(embedding) USING HNSW;

-- 状态查询索引
CREATE INDEX idx_wiki_embed_status ON wiki_page(vault_id, embed_status);
```

### 3.2 状态机

```
PENDING（初始） ─────┬───→ SUCCESS（向量已生成）
                    └───→ FAILED（API 调用失败）
                              ↓ 用户重试
                          PENDING（重新进入队列）
```

---

## 四、配置项设计

### 4.1 新增 Setting Key

在 `app_setting` 表中新增以下配置（key, type, default）：

| Key | Type | Default | 说明 |
|-----|------|---------|------|
| `llm.embedding_enabled` | BOOLEAN | `false` | 总开关 |
| `llm.embedding_base_url` | STRING | `https://dashscope.aliyuncs.com/compatible-mode/v1` | API 地址 |
| `llm.embedding_api_key` | SECRET | `` | API Key（脱敏存储） |
| `llm.embedding_model` | STRING | `text-embedding-v3` | 模型名 |
| `llm.embedding_dimension` | INTEGER | `1024` | 向量维度 |
| `llm.embedding_batch_size` | INTEGER | `25` | 批量调用每批大小（DashScope 上限 25） |

### 4.2 兼容性

- `embedding_enabled = false` 时，搜索逻辑走纯关键词检索（现有逻辑不变）
- `embedding_enabled = true` 但 API Key 未配置时，`/api/wiki/search` 返回友好错误："请先在设置中配置 Embedding API Key"

---

## 五、后端实现

### 5.1 新建包结构

```
domain/embedding/
  ├── EmbeddingConfig.java           # 配置 POJO
  ├── EmbeddingClient.java           # HTTP 客户端，调 OpenAI 兼容 /v1/embeddings
  ├── EmbeddingTextBuilder.java      # 从 WikiPage 构造向量化文本
  └── EmbeddingService.java          # 业务编排（含 hash 判断、批量、状态机）

domain/search/
  ├── HybridSearchService.java       # 新增：混合检索 + RRF
  ├── VectorSearchService.java       # 新增：MySQL 向量查询封装
  └── KeywordSearchService.java      # 已存在，不改动
```

### 5.2 EmbeddingConfig.java

```java
package com.jihao.aiwiki.domain.embedding;

@Data
@Builder
public class EmbeddingConfig {
    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
    private int dimension;
    private int batchSize;

    /** 从 SettingService 加载 */
    public static EmbeddingConfig load(SettingService settingService, Long vaultId) {
        // 读取上述 6 个配置项
    }
}
```

### 5.3 EmbeddingClient.java

```java
@Component
public class EmbeddingClient {

    /**
     * 批量向量化文本。
     * 请求：POST {baseUrl}/embeddings
     * Body: {"model": "...", "input": [text1, text2, ...], "encoding_format": "float"}
     * Response: {"data": [{"embedding": [...]}, ...]}
     *
     * @param texts 待向量化文本列表（长度 <= batchSize）
     * @param config 配置
     * @return 向量数组，与 texts 一一对应
     * @throws EmbeddingException API 调用失败
     */
    public List<float[]> embed(List<String> texts, EmbeddingConfig config);

    /**
     * 单文本向量化（用户 query 场景）。
     */
    public float[] embedSingle(String text, EmbeddingConfig config);
}
```

**实现要点：**
- 使用 `RestTemplate` 或 `WebClient`
- 请求超时：连接 10s，读取 30s
- 失败重试：指数退避，最多 2 次
- 文本截断上限：2000 字符（防止超 token 限制）

### 5.4 EmbeddingTextBuilder.java

```java
@Component
public class EmbeddingTextBuilder {

    /**
     * 从 WikiPage 构造向量化文本。
     * 规则：title + "\n" + summary（前 500 字）+ "\n" + body（前 1500 字）
     * 总长度 <= 2000 字符
     */
    public String build(WikiPage page);

    /** 计算内容 hash，用于增量判断 */
    public String contentHash(WikiPage page);  // SHA-256(title + summary + body[0:1500])
}
```

### 5.5 EmbeddingService.java

```java
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingClient client;
    private final EmbeddingTextBuilder textBuilder;
    private final WikiPageMapper mapper;
    private final SettingService settingService;

    /**
     * 单页面向量化（同步）。
     * 1. 加载页面 → 计算 contentHash
     * 2. 若 hash 与 embed_content_hash 相同 → 跳过
     * 3. 调 EmbeddingClient → 更新 DB（embedding, embed_status=SUCCESS, embed_content_hash, embedded_at）
     * 4. 失败 → 更新 DB（embed_status=FAILED, embed_error）
     */
    public EmbeddingResult embedPage(Long pageId);

    /**
     * 批量向量化（异步）。分批调用，每批 batchSize 条。
     * 实时更新进度到 Redis：key = "embedding:progress:{vaultId}"
     */
    @Async("embeddingExecutor")
    public void embedPagesBatch(List<Long> pageIds);

    /**
     * 用户查询向量化（同步，无 hash 检查）。
     */
    public float[] embedQuery(String query, Long vaultId);

    /**
     * 统计当前 vault 的向量化状态。
     */
    public EmbeddingStats stats(Long vaultId);  // {total, success, failed, pending}
}
```

### 5.6 HybridSearchService.java

```java
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final KeywordSearchService keywordSearchService;
    private final WikiPageMapper mapper;

    /**
     * 混合检索：向量 top 20 + 关键词 top 20 → RRF 融合 → 返回 topK。
     *
     * 兜底逻辑：
     * - embedding_enabled=false → 走纯关键词
     * - embedding_enabled=true 但所有页面都未向量化 → 走纯关键词
     * - 向量调用失败 → 退回关键词
     */
    public List<ScoredPage> search(Long vaultId, String query, int topK);

    /**
     * RRF 融合算法。
     * score(doc) = Σ 1 / (k + rank_i)，k=60
     */
    private List<ScoredPage> rrfFusion(
        List<ScoredPage> vectorResults,
        List<ScoredPage> keywordResults,
        int topK
    );
}
```

### 5.7 VectorSearchService.java + Mapper

**Java：**
```java
@Component
public class VectorSearchService {
    public List<ScoredPage> search(Long vaultId, float[] queryVec, int topK);
}
```

**`WikiPageMapper.xml` 新增：**
```xml
<select id="vectorSearch" resultType="com.jihao.aiwiki.domain.search.ScoredPage">
    SELECT
        id, vault_id, path, title, summary,
        VECTOR_DISTANCE(embedding, #{queryVec}) AS distance
    FROM wiki_page
    WHERE vault_id = #{vaultId}
      AND embed_status = 'SUCCESS'
      AND embedding IS NOT NULL
    ORDER BY distance ASC
    LIMIT #{topK}
</select>
```

---

## 六、API 接口

### 6.1 向量管理 API

```
GET    /api/embedding/stats?vaultId={id}
       返回：{total, success, failed, pending, lastEmbeddedAt}

POST   /api/embedding/test
       Body: {baseUrl, apiKey, model}
       作用：测试 Embedding 配置是否可用，返回向量维度

POST   /api/embedding/rebuild
       Body: {vaultId, mode: "pending"|"failed"|"all"}
       作用：异步触发批量向量化，返回 taskId

GET    /api/embedding/progress?vaultId={id}
       返回：{processing: bool, current: 30, total: 120}（从 Redis 读取）

POST   /api/wiki/{pageId}/embed
       作用：手动重新向量化单个页面（同步）
```

### 6.2 检索 API（修改）

```
GET    /api/wiki/search?vaultId={id}&keyword={kw}
       内部逻辑切换：调 HybridSearchService 而非 KeywordSearchService
       响应结构不变（保持向后兼容）
```

---

## 七、前端实现

### 7.1 新增页面：向量管理

**文件：** `frontend/src/pages/EmbeddingPage.vue`

**菜单位置：** 主菜单「设置」之上，新增「向量管理」入口

**页面布局：**
```
┌─────────────────────────────────────────────────┐
│ 向量管理                                         │
├─────────────────────────────────────────────────┤
│ 状态统计                                         │
│   总页面: 120   已向量化: 80   待处理: 35  失败: 5│
│   [██████░░░░] 67% 已完成                       │
│   最近向量化: 2026-05-08 10:30                  │
├─────────────────────────────────────────────────┤
│ 操作                                             │
│   [向量化未处理页面 (35)]                        │
│   [重试失败页面 (5)]                             │
│   [全部重新向量化 (120)]   ⚠️ 会消耗 API 配额    │
├─────────────────────────────────────────────────┤
│ 失败列表                                         │
│   ❌ wiki/concepts/xxx.md                       │
│      错误: API timeout                          │
│      [重试] [查看页面]                          │
└─────────────────────────────────────────────────┘
```

### 7.2 设置页扩展

**文件：** `frontend/src/pages/SettingsPage.vue`

在 LLM 配置下方新增「Embedding 配置」区块：

```
┌─────────────────────────────────────────────────┐
│ Embedding 配置（用于向量检索）                  │
│ ☑ 启用 Embedding                                │
│                                                 │
│ Base URL  [https://dashscope.aliyuncs.com/...] │
│ API Key   [********************************]   │
│ Model     [text-embedding-v3]                   │
│ Dimension [1024]                                │
│                                                 │
│ [保存配置]  [测试连通性]                        │
└─────────────────────────────────────────────────┘
```

### 7.3 摄入队列页通知

**文件：** `frontend/src/pages/TasksPage.vue`

任务完成后，若该 vault 有 `embed_status != SUCCESS` 的页面，顶部展示横幅：

```
💡 共 5 个 Wiki 页面未向量化  [前往向量管理 →]
```

### 7.4 Dashboard 提醒

**文件：** `frontend/src/pages/DashboardPage.vue`

在统计卡片下方新增提醒卡：
```
🟡 Embedding 状态
   5 个页面待向量化   [立即处理]
```

仅当 `embedding_enabled=true` 且有待处理页面时显示。

### 7.5 API Client 扩展

**文件：** `frontend/src/api/client.ts`

新增：
```typescript
export const embeddingApi = {
  stats(vaultId: number): Promise<EmbeddingStats>,
  test(config: EmbeddingTestConfig): Promise<{dimension: number}>,
  rebuild(vaultId: number, mode: "pending" | "failed" | "all"): Promise<{taskId: string}>,
  progress(vaultId: number): Promise<EmbeddingProgress>,
  embedPage(pageId: number): Promise<void>,
}
```

---

## 八、异步任务与进度跟踪

### 8.1 线程池配置

```java
@Configuration
public class AsyncConfig {
    @Bean("embeddingExecutor")
    public Executor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("embedding-");
        return executor;
    }
}
```

### 8.2 进度追踪

使用 Redis 存储进度：
```
Key: embedding:progress:{vaultId}
Value: {"processing": true, "current": 30, "total": 120, "startedAt": ...}
TTL: 1 小时
```

### 8.3 并发控制

- 同一 vault 同时只允许一个批量向量化任务运行
- 用 Redis SETNX 加锁：`embedding:lock:{vaultId}`，TTL 30 分钟

---

## 九、错误处理

| 场景 | 处理 |
|------|------|
| API Key 无效 | 标记 FAILED，error 写入"API Key invalid" |
| 网络超时 | 重试 2 次（指数退避），仍失败标记 FAILED |
| 超过 token 限制 | 客户端预先截断到 2000 字符，避免触发 |
| 维度不匹配 | 启动时校验 config.dimension 与实际返回维度，不一致则报错并禁用 |
| MySQL VECTOR 不支持 | 启动时检测 MySQL 版本 < 8.4 时禁用功能并日志告警 |

---

## 十、性能与成本

### 10.1 性能预估

- DashScope `text-embedding-v3` 单次调用 ~ 200ms
- 批量 25 条 / 次 → 100 篇文档约 4 次调用 ~ 1 秒
- HNSW 向量检索 100k 条 < 50ms

### 10.2 成本预估

- DashScope `text-embedding-v3`：¥0.0007 / 千 token
- 单篇文档约 500 token → ¥0.00035 / 篇
- 1000 篇文档 → ¥0.35
- **结论：成本极低，但仍需用户主动触发避免误操作**

---

## 十一、实施任务列表

按依赖顺序执行：

### Phase 1: 数据层与配置（半天）
- [ ] **任务 1.1** 编写 V002 Flyway 迁移
- [ ] **任务 1.2** `WikiPageDO` 添加新字段
- [ ] **任务 1.3** SettingService 新增 6 个 embedding 配置项
- [ ] **任务 1.4** 启动时校验 MySQL 8.4+

### Phase 2: Embedding 核心（半天）
- [ ] **任务 2.1** `EmbeddingConfig` POJO
- [ ] **任务 2.2** `EmbeddingClient`（含重试、超时、错误处理）
- [ ] **任务 2.3** `EmbeddingTextBuilder`
- [ ] **任务 2.4** `EmbeddingService`（含 hash 判断、状态机）
- [ ] **任务 2.5** AsyncConfig 线程池

### Phase 3: 检索改造（半天）
- [ ] **任务 3.1** `WikiPageMapper.vectorSearch` SQL
- [ ] **任务 3.2** `VectorSearchService`
- [ ] **任务 3.3** `HybridSearchService`（含 RRF + 兜底）
- [ ] **任务 3.4** `WikiController.search` 切换调用

### Phase 4: API 与异步任务（半天）
- [ ] **任务 4.1** `EmbeddingController`（5 个接口）
- [ ] **任务 4.2** Redis 进度追踪 + 并发锁
- [ ] **任务 4.3** 失败重试机制

### Phase 5: 前端集成（1 天）
- [ ] **任务 5.1** `EmbeddingPage.vue`（向量管理页）
- [ ] **任务 5.2** `SettingsPage.vue` 新增 Embedding 区块 + 测试按钮
- [ ] **任务 5.3** `TasksPage.vue` 完成通知横幅
- [ ] **任务 5.4** `DashboardPage.vue` 提醒卡片
- [ ] **任务 5.5** `client.ts` 新增 `embeddingApi`
- [ ] **任务 5.6** 路由 + 主菜单入口

### Phase 6: 验收（半天）
- [ ] **任务 6.1** 端到端测试：配置 → 测试连通性 → 批量向量化 → 检索验证
- [ ] **任务 6.2** 失败场景测试：错误的 API Key、超时、维度不匹配
- [ ] **任务 6.3** 性能测试：1000 篇文档向量化耗时

**总计预估：约 3 天**

---

## 十二、回滚方案

若需回滚：
1. 设置中关闭 `embedding_enabled` → 立即退回纯关键词检索
2. 数据无需删除（VECTOR 字段不影响其他字段读写）
3. Flyway 不支持自动回滚 SQL，需手动 DROP COLUMN 与 INDEX（如需）

---

## 十三、未来可扩展点

- **多模态 Embedding**：图片、表格的向量化
- **chunk 级检索**：将长文档切分为段落，每段独立向量化
- **重排序模型**：在 RRF 之上加 ReRanker（如 Cohere、bge-reranker）
- **跨 vault 检索**：当前限定单 vault，可扩展为全局
- **本地 Embedding**：用 sentence-transformers 等本地模型替代 API 调用

---

## 附录 A：DashScope 接口示例

**请求：**
```bash
curl -X POST https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings \
  -H "Authorization: Bearer sk-xxx" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "text-embedding-v3",
    "input": ["文本1", "文本2"],
    "encoding_format": "float",
    "dimensions": 1024
  }'
```

**响应：**
```json
{
  "data": [
    {"embedding": [0.123, -0.456, ...], "index": 0},
    {"embedding": [0.789, -0.012, ...], "index": 1}
  ],
  "model": "text-embedding-v3",
  "usage": {"total_tokens": 12}
}
```

---

## 附录 B：MySQL 8.4 VECTOR 用法

```sql
-- 创建表
CREATE TABLE example (
  id BIGINT PRIMARY KEY,
  vec VECTOR(1024)
);

-- 插入（VECTOR 接受 JSON 数组字符串）
INSERT INTO example VALUES (1, '[0.1, 0.2, ...]');

-- 查询相似（cosine distance）
SELECT id, VECTOR_DISTANCE(vec, '[0.1, 0.2, ...]') AS d
FROM example
ORDER BY d ASC
LIMIT 10;

-- HNSW 索引
CREATE VECTOR INDEX idx_vec ON example(vec) USING HNSW;
```
