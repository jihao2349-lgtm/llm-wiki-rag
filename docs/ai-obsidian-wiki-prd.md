# AI Obsidian Wiki 知识库系统 PRD

版本：v0.2  
日期：2026-05-06  
状态：需求草案（v0.2 增加多模态投喂与外部 AI 调用场景）

变更记录：

- v0.1（2026-04-30）：MVP 需求草案，覆盖 Vault 初始化、资料导入、AI 摄入、Web Chat。
- v0.2（2026-05-06）：新增多模态投喂、外部 AI 调用（REST + MCP）、向量检索演进路线。

## 1. 背景

用户希望基于自己的 Obsidian Vault 构建一个 Web 端 AI 知识库系统。系统支持导入文件和网页链接，自动解析资料内容，调用 AI 生成结构化 Markdown Wiki，并保存到 Obsidian Vault 中。同时，用户可以在 Web 页面中基于整个 Vault 与 AI 对话，AI 能检索、引用、汇总已有知识。

本产品不替代 Obsidian，而是作为 Obsidian 的 AI 增强层。

## 2. 产品目标

核心链路：

```text
资料导入 -> 内容解析 -> AI 摄入 -> 生成 Obsidian Wiki -> AI 检索对话
```

第一版重点解决三个问题：

1. 用户可以方便地把文件、网页资料导入知识库。
2. AI 能把资料整理成 Obsidian 可用的 Markdown 页面。
3. 用户可以基于 Obsidian Vault 进行 AI 问答，并看到引用来源。

第一版不追求完整桌面软件、复杂图谱、多模型工作流和全自动知识治理。

## 3. 目标用户

主要用户：

- 有 Obsidian 使用习惯的个人知识管理用户。
- 经常阅读 PDF、网页、文章、报告、技术文档的人。
- 希望用 AI 自动整理知识，而不是手动摘抄的人。
- 需要基于自己资料进行问答、总结、复盘的人。

典型场景：

- 导入一篇 PDF 论文，AI 自动生成摘要、概念页、实体页。
- 导入一个网页链接，AI 解析正文并写入 Obsidian。
- 用户提问：“我之前收集的资料里，关于 Agent Memory 的观点有哪些？”
- AI 检索 Vault，汇总答案，并引用相关 Markdown 页面。

## 4. 产品边界

本产品做：

- 管理 Obsidian Vault 路径。
- 导入文件和网页链接。
- 解析资料文本、表格、图片引用。
- 调用 AI 生成 Markdown Wiki。
- 写入 Obsidian Vault。
- 建立索引并支持 AI 对话。
- 提供任务状态和失败重试。

本产品不做：

- 不重新实现 Obsidian 编辑器。
- 不做复杂笔记排版。
- 不做多人协作。
- 不做云同步。
- 不直接修改用户大量已有笔记，默认生成新页面或草稿。
- 第一版不做 Chrome 插件、复杂知识图谱、自动社区检测。

## 5. 信息架构

推荐 Vault 结构：

```text
vault/
  purpose.md
  schema.md

  raw/
    sources/
    assets/

  wiki/
    index.md
    log.md
    overview.md
    sources/
    entities/
    concepts/
    questions/
    synthesis/

  .ai-wiki/
    config.json
    queue.json
    ingest-cache.json
    review.json
```

目录说明：

- `purpose.md`：知识库目标，告诉 AI 这个库为什么存在。
- `schema.md`：AI 生成规则，定义页面类型、命名、frontmatter、链接规则。
- `raw/sources/`：保存原始资料。
- `raw/assets/`：保存附件、图片、资源文件。
- `wiki/`：保存 AI 生成和维护的 Markdown 页面。
- `.ai-wiki/`：保存系统内部状态，不作为 Obsidian 主要阅读内容。

## 6. 核心页面

### 6.1 Dashboard

目的：让用户快速了解当前知识库状态。

功能：

- 显示当前 Vault 名称和路径。
- 显示资料数量、Wiki 页面数量、最近摄入任务。
- 提供“导入资料”“开始对话”入口。

### 6.2 Sources

目的：管理原始资料导入。

功能：

- 上传文件。
- 输入网页 URL。
- 查看已导入资料。
- 查看解析状态。
- 手动触发 AI 摄入。

### 6.3 Tasks

目的：展示摄入队列。

功能：

- 显示待处理、处理中、成功、失败任务。
- 支持取消任务。
- 支持失败任务重试。
- 展示错误原因和已写入文件。

### 6.4 Chat

目的：基于 Obsidian Vault 与 AI 对话。

功能：

- 用户输入问题。
- AI 返回流式答案。
- 展示引用来源。
- 点击引用查看 Markdown 页面。
- 支持保存回答到 Wiki。

### 6.5 Wiki

目的：Web 端预览生成的 Wiki 内容。

功能：

- 显示 `wiki/` 文件树。
- 预览 Markdown。
- 展示 frontmatter。
- 支持打开对应 Obsidian 文件路径。

### 6.6 Settings

目的：配置系统运行参数。

功能：

- 配置 Vault 路径。
- 配置 LLM API。
- 配置 embedding 开关。
- 配置输出语言。

## 7. 核心流程：初始化 Vault

用户选择一个本地目录作为 Obsidian Vault。

系统检查：

- 目录是否存在。
- 目录是否可读写。
- 是否已有 `purpose.md`。
- 是否已有 `schema.md`。
- 是否已有 `wiki/index.md`。

如果没有，系统创建默认结构。

验收标准：

- 用户可以成功绑定一个已有 Vault。
- 系统不会破坏已有 `.obsidian/` 配置。
- 初始化后，Obsidian 可以直接打开该目录。

## 8. 核心流程：导入资料

支持类型：

```text
PDF
DOCX
PPTX
XLSX
Markdown
TXT
HTML
网页 URL
图片文件
CSV
JSON
```

导入步骤：

1. 用户上传文件或输入 URL。
2. 系统保存原始资料到 `raw/sources/`。
3. 系统解析正文。
4. 解析结果生成 SourceDocument 记录。
5. 用户可预览解析文本。
6. 用户点击“AI 摄入”，或系统自动加入队列。

解析要求：

- PDF 能提取主要文本。
- 网页 URL 能提取标题、正文、原链接。
- Office 文档能提取基础文本和表格文本。
- 图片第一版只保存引用，可选后续加入视觉 caption。
- 文件名冲突时自动重命名。
- 解析失败时保留原始文件，并展示失败原因。

## 9. 核心流程：AI 摄入

摄入采用两阶段。

### 9.1 第一阶段：资料分析

输入：

- `purpose.md`
- `schema.md`
- `wiki/index.md`
- 原始资料文本
- 解析出的表格内容
- 图片引用或 caption，第一版可选

输出：

- 资料摘要。
- 核心实体。
- 核心概念。
- 关键观点。
- 和现有 Wiki 的关联。
- 矛盾点。
- 需要人工判断的问题。
- 建议生成或更新的页面。

### 9.2 第二阶段：生成 Wiki 文件

AI 必须输出 FILE block：

```text
---FILE: wiki/sources/source-name.md---
---
type: source
title: Source Name
sources:
  - raw/sources/source-name.pdf
tags: []
related: []
created: 2026-04-30
updated: 2026-04-30
---

# Source Name

...
---END FILE---
```

系统解析 FILE block 后写入 Vault。

安全规则：

- 只允许写入 `wiki/`。
- 禁止绝对路径。
- 禁止 `../`。
- 写入前校验 Markdown frontmatter。
- 写入 `index.md`、`overview.md` 前保留备份或历史版本。
- 摄入失败不应留下不可识别的半成品。
- LLM 不确定的问题写入 Review Queue，不直接覆盖结论。

验收标准：

- 导入一篇资料后，至少生成一个 `wiki/sources/*.md` 页面。
- 新概念或实体会生成到对应目录。
- `wiki/index.md` 被更新。
- `wiki/log.md` 记录本次摄入。
- Obsidian 内可直接看到生成页面和 `[[wikilink]]`。

## 10. 核心流程：AI 对话

用户在 Chat 页面提问。

系统处理：

```text
用户问题
  -> 搜索相关 Markdown 页面
  -> 读取页面内容
  -> wikilink 一跳扩展
  -> 控制上下文长度
  -> 调用 LLM
  -> 返回答案和引用来源
```

第一版检索策略：

- 文件名匹配。
- 标题匹配。
- 正文关键词匹配。
- `[[wikilink]]` 一跳扩展。
- embedding 可作为增强项，不作为 MVP 必需。

回答规则：

- AI 必须基于检索到的内容回答。
- 信息不足时明确说明。
- 返回引用来源，例如 `[1] wiki/concepts/agent-memory.md`。
- 支持保存回答为 `wiki/synthesis/` 或 `wiki/questions/` 页面。

验收标准：

- 用户提问后可以开始返回流式内容，具体耗时取决于模型。
- 答案展示引用页面。
- 点击引用可以打开 Markdown 预览。
- 用户可以把答案保存到 Vault。

## 11. 任务队列需求

摄入任务必须进入队列，不允许多个任务同时写同一个 Vault。

任务字段：

- `taskId`
- `sourceId`
- `status`
- `progress`
- `retryCount`
- `errorMessage`
- `createdAt`
- `updatedAt`
- `writtenFiles`

状态：

```text
Pending
Processing
Done
Failed
Cancelled
```

规则：

- 默认串行处理。
- 失败自动重试 3 次。
- 用户可手动重试。
- 服务重启后可恢复未完成任务。
- 任务失败不能影响其他任务。

## 12. 数据模型简版

第一版数据库可以使用 SQLite，后续再切 PostgreSQL。

核心表：

```text
vault_project
- id
- name
- path
- created_at
- updated_at

source_document
- id
- vault_id
- type
- title
- original_path
- extracted_text_path
- status
- created_at

wiki_page
- id
- vault_id
- path
- title
- type
- tags
- updated_at

ingest_task
- id
- source_id
- status
- error_message
- written_files
- created_at
- updated_at

chat_session
- id
- vault_id
- title
- created_at

chat_message
- id
- session_id
- role
- content
- references
- created_at
```

## 13. LLM 配置

第一版稳定支持 OpenAI-compatible API。主推 **DeepSeek 系列**，本地推理用 Ollama。

配置项：

- `provider`
- `baseUrl`
- `apiKey`
- `model`
- `maxContextSize`
- `temperature`
- `outputLanguage`

优先级：

1. **DeepSeek**（OpenAI-compatible，主推：`deepseek-chat`、`deepseek-reasoner`）
2. 其它 OpenAI-compatible Provider（DashScope、自建网关等）
3. Ollama，本地模型作为离线兜底（推荐 `deepseek-r1:14b`、`qwen3:14b`）

理由：

- DeepSeek 走标准 OpenAI 协议，零代码兼容；Pricing 比 GPT/Claude 低一个数量级，长文摄入成本可控。
- `deepseek-reasoner` 适合阶段一的"资料分析 + 矛盾点识别"，`deepseek-chat` 适合阶段二的 FILE block 生成与 Chat 流式回答。
- 不要一开始接太多模型，否则 prompt 调优和维护成本过高。

## 14. 非功能需求

性能：

- 1000 篇 Markdown 内，关键词搜索应在 1 秒内完成。
- 大文件解析走后台任务。
- 对话支持流式响应。

安全：

- 所有写文件操作限制在 Vault 路径内。
- LLM 输出路径必须校验。
- 不允许覆盖 `raw/` 原始文件。
- 删除操作需要确认。

可靠性：

- 任务失败可重试。
- 服务重启后队列可恢复。
- 写入 Wiki 页面前保留可追踪记录。

可维护性：

- prompt 模板独立管理。
- 文件解析、AI 摄入、索引、对话模块解耦。
- 所有生成文件遵守 Obsidian Markdown 兼容规则。

## 15. MVP 范围

MVP 必须完成：

- Vault 初始化和绑定。
- 文件上传。
- URL 导入。
- PDF、TXT、Markdown 基础解析。
- AI 两阶段摄入。
- 写入 Obsidian Markdown。
- 更新 `index.md`、`log.md`。
- 任务队列。
- 基础关键词检索。
- AI 对话和引用来源。
- 保存回答到 Wiki。

MVP 不包含：

- Chrome 插件。
- 高级图谱。
- 图片视觉理解。
- 表格深度结构化分析。
- 多人协作。
- 云端同步。
- 自动合并已有笔记。

## 16. 成功指标

第一版成功标准：

- 用户能在 5 分钟内完成 Vault 初始化。
- 用户能导入一篇 PDF 或网页，并看到生成的 Obsidian Markdown 页面。
- 用户能基于已导入资料完成一次 AI 问答。
- AI 答案能展示至少一个有效引用来源。
- 生成的 Wiki 页面可以直接在 Obsidian 中阅读和链接。

## 17. 推荐开发顺序

1. 后端 Vault 文件读写。
2. 前端 Vault 初始化和文件树展示。
3. 文件和 URL 导入。
4. 文本解析。
5. LLM 配置和测试调用。
6. AI 摄入 FILE block 写入。
7. 任务队列。
8. Wiki 预览。
9. 基础搜索。
10. AI 对话。
11. 保存回答到 Wiki。
12. Review 和索引增强。

## 18. 借鉴 llm_wiki 的工程实践

需要直接借鉴：

- raw 和 wiki 分离。
- `schema.md` 控制生成规则。
- `purpose.md` 控制知识库方向。
- 摄入使用两阶段 LLM。
- 写文件必须走 FILE block 协议。
- 只允许写 `wiki/`，防止路径穿越。
- 摄入队列串行化。
- 生成结果必须可被 Obsidian 直接打开。
- 对话必须有检索、引用和上下文预算。
- 不确定内容进入 review，而不是让 AI 擅自决定。

暂不借鉴：

- 完整 Tauri 桌面应用。
- 多 Provider 复杂适配。
- Louvain 社区检测。
- 多模态图片 caption。
- Chrome 剪藏插件。
- 复杂 Deep Research 流程。

## 19. 一句话总结

本项目不是给 Obsidian 加一个简单聊天框，而是做一个独立的 AI 知识编译层：它读取文件和网页，调用 LLM 把资料编译成 Obsidian Markdown Wiki，再基于这些 Wiki 页面做检索、推理、总结和持续维护。Obsidian 负责长期存储和人工编辑，Web 项目负责 AI 自动化。

---

# v0.2 扩展需求

## 20. 场景定位扩展

v0.1 默认场景是用户在 Web Chat 与 AI 对话。v0.2 明确支持两类调用方：

| 场景 | 用户 | 调用方 | 入口 |
|------|------|--------|------|
| A. 项目内 Chat | 真人 | Web 浏览器 | 本项目 Chat 页面 |
| B. 外部 AI 调用 | AI Agent / 第三方应用 | LLM / Coding Agent / 自动化脚本 | HTTP API / MCP Server |

两个场景共享同一个知识库和检索引擎，但暴露层不同：

- 场景 A 关注用户体验：流式答案、引用展示、答案沉淀。
- 场景 B 关注接口稳定性：结构化 JSON、鉴权配额、可机读元数据。

## 21. 多模态投喂

v0.1 的导入资料章节只覆盖文本类型。v0.2 明确多模态处理策略。

支持范围：

| 模态 | v0.1 处理 | v0.2 处理 |
|------|----------|----------|
| PDF / DOCX / PPTX | 文本 + 表格抽取 | 同 v0.1 |
| 图片（独立或嵌入 PDF） | 仅保存引用 | VLM 生成 caption + OCR 文字提取 |
| 音频 | 不支持 | Whisper 转写 + 时间戳分段 |
| 视频 | 不支持 | 抽帧 + Whisper 转写 + 关键帧 caption |
| 网页 URL | Jsoup 抽正文 | 增加截图 + VLM 理解关键图表 |
| 代码仓库 / 文件夹 | 不支持 | 递归扫描，按文件路径生成结构化 Markdown |

关键约束：

- 所有多模态内容最终都落地为 Markdown 正文 + frontmatter 元数据，不引入二进制索引格式。
- 原始多模态资料保存到 `raw/assets/`，提取后的文本作为 Wiki 页面正文。
- 提取过程是异步任务，进入摄入队列，与文本资料共用同一套状态机。
- 提取失败时保留原始资料，标记 `extraction_status` 为 `FAILED`，不阻断后续摄入。

新增 frontmatter 字段：

```yaml
modality: [text|image|audio|video|mixed]
asset_refs:
  - raw/assets/lecture-2026-04.mp3
extraction_method: whisper-large-v3
extracted_at: 2026-05-06
```

## 22. 场景 A 增强：项目内 Chat

在 v0.1 的基础上新增三项能力。

### 22.1 多轮对话记忆

- 每个 `chat_session` 维护短期记忆，最近 N 轮对话拼入上下文。
- 用户可以"固定"某条历史消息作为长期上下文，后续每轮都注入。
- 上下文预算策略：固定消息 > 最近消息 > 检索片段，按优先级截断。

### 22.2 主动推荐

- 用户提问后，除了答案还返回：
  - 相关 Wiki 页面（即使未被引用）
  - 未覆盖的子问题（信息不足时由 AI 主动列出）
- 用于引导用户继续投喂资料、补充知识盲区。

### 22.3 答案沉淀升级

v0.1 的"保存到 Wiki"扩展为三种模式：

- 保存为 `wiki/synthesis/`：合成观点页面
- 保存为 `wiki/questions/`：开放问题，等待后续资料补充
- 追加到已有页面的"补充"段落：不覆盖原内容，便于增量积累

所有保存动作仍走 FILE block 协议和路径校验。

## 23. 场景 B：外部 AI 调用

### 23.1 调用方画像

预期三类调用方：

1. **Coding Agent**：主要目标是 OpenAI **Codex CLI**（已原生支持 MCP），其次兼容 Claude Code、Cursor 等支持 MCP 的工具。
2. **AI 应用后端**：通过 REST API 拉取 RAG 上下文，后端模型推荐使用 DeepSeek 走 OpenAI-compatible 协议。
3. **自动化脚本**：批量查询、定时同步、外部投喂。

为什么主推 Codex CLI：

- 用户主要工作流以编码为主，Codex CLI 是当前 OpenAI 官方编码代理，原生支持 MCP stdio。
- 与后端 DeepSeek 模型搭配可以做到"前端 Codex 调度 + 后端 DeepSeek 推理"的低成本组合。
- Claude Code、Cursor 同样支持 MCP，用户可任意切换，本系统不做客户端绑定。

### 23.2 双协议设计

#### REST API

| 接口 | 用途 |
|------|------|
| `POST /api/v1/rag/search` | 给定 query，返回 top-k 相关 Markdown 片段 + 引用 |
| `POST /api/v1/rag/answer` | 给定 query，返回 LLM 生成的答案 + 引用 |
| `GET /api/v1/rag/page?path=` | 直接读取指定 Wiki 页面原文 |
| `GET /api/v1/rag/list?type=concept` | 按类型列出 Wiki 页面 |
| `POST /api/v1/rag/ingest` | 外部应用投喂资料（异步任务） |

设计原则：

- 所有接口返回结构化 JSON，包含 `chunks[]`、`references[]`、`metadata{}`。
- 支持 `mode=retrieval`（只返回片段）和 `mode=answer`（返回 LLM 答案）两种调用风格。
- 通过 API Key 鉴权，区别于 LLM Provider 的 API Key。
- 与 v0.1 内部 API 路径区分：内部用 `/api/`，外部用 `/api/v1/rag/`。

#### MCP Server

把 Vault 包装成 MCP Server，让 Claude Code、Cursor 等 Agent 直接调用：

| MCP Tool | 等价 REST 接口 |
|----------|---------------|
| `wiki_search(query)` | `/api/v1/rag/search` |
| `wiki_read(path)` | `/api/v1/rag/page` |
| `wiki_list(type)` | `/api/v1/rag/list` |
| `wiki_ingest(content, source)` | `/api/v1/rag/ingest` |

MCP Server 与 REST API 共用同一个 Service 层，只是协议适配层不同。

### 23.3 RAG 检索接口契约示例

```json
// Request
{
  "query": "Agent Memory 的设计模式",
  "topK": 5,
  "filters": {
    "type": ["concept", "synthesis"],
    "tags": ["agent"],
    "updatedAfter": "2026-01-01"
  },
  "expandWikilinks": true,
  "maxTokens": 4000
}

// Response
{
  "code": 200,
  "data": {
    "chunks": [
      {
        "path": "wiki/concepts/agent-memory.md",
        "title": "Agent Memory",
        "type": "concept",
        "score": 0.87,
        "content": "Agent Memory 通常分为三层...",
        "frontmatter": { "tags": ["agent"], "updated": "2026-04-30" },
        "wikilinks": ["wiki/concepts/short-term-memory.md"]
      }
    ],
    "totalTokens": 3580,
    "queryId": "q-20260506-001"
  }
}
```

### 23.4 鉴权与配额

- API Key 在 Settings 页面生成，存储为加密哈希，不可逆查看。
- 每个 Key 配置：QPS 上限、月调用量上限、可访问的 Vault 范围、可调用的接口范围（scopes）。
- 所有外部调用记录到 `api_call_log` 表，便于审计。
- Key 可以撤销、过期、临时禁用。

## 24. 数据模型扩展

### 24.1 新增表

```text
api_key
- id
- vault_id
- name
- key_hash
- key_prefix
- qps_limit
- monthly_quota
- scopes
- enabled
- expires_at
- created_at

api_call_log
- id
- api_key_id
- endpoint
- query_text
- response_tokens
- latency_ms
- status
- created_at

asset
- id
- vault_id
- source_id
- asset_path
- modality
- extraction_status
- extracted_text
- metadata
- created_at
```

### 24.2 现有表扩展

`wiki_page` 增加：

- `modality`：页面主模态，默认 `text`。
- `summary`：一句话摘要，给 RAG 召回用，便于快速预览。
- `embedding_id`：向量库主键，可选，未启用 embedding 时为空。

`source_document` 增加：

- `modality`：原始资料模态。
- `extraction_method`：使用的提取工具或模型。

## 25. 前端页面调整

### 25.1 Settings 页面扩展

新增两个 Tab：

- **API Keys 管理**：生成、撤销外部 Key，查看每个 Key 的调用统计。
- **MCP 配置**：显示 MCP Server 启动命令和配置 JSON，用户可复制到 Claude Code、Cursor 等工具。

### 25.2 新增 API Console 页面

面向开发者：

- 在线测试 RAG 接口（输入 query、查看响应）。
- 查看最近 API 调用日志，支持按 Key 筛选。
- 导出 OpenAPI Spec，便于第三方对接。

## 26. 检索引擎演进

为了让两个场景共享检索质量，v0.2 引入向量检索。

```text
关键词召回（v0.1 已有）+ 向量召回（v0.2 新增）+ wikilink 扩展
                       ↓
                Reranker 排序（可选）
                       ↓
                Top-K 上下文组装
```

技术选择建议：

| 组件 | 推荐方案 | 备选 |
|------|---------|------|
| 向量模型 | bge-m3（中英多语言） | text-embedding-3-small |
| 向量库 | PostgreSQL + pgvector 或 Qdrant | Milvus |
| 关键词索引 | MySQL FULLTEXT 或 Lucene | Elasticsearch |
| Reranker | bge-reranker-v2，可选 | 不开 |

约束：

- 向量检索是增强项，不替换关键词检索；两者并行召回再融合。
- 用户在 Settings 中可关闭向量检索，回退到 v0.1 模式。
- 向量库部署独立于 MySQL，避免污染主数据库。

## 27. MVP 范围与版本路线

调整后版本路线：

**v0.1（MVP）**：

- 沿用原 v0.1 PRD 全部内容。
- 场景 A 完整可用。
- 场景 B 暂不实现，但 Service 层接口签名预留。

**v0.2**：

- 多模态投喂：图片 OCR + Caption、音视频转写。
- 场景 B：REST API + API Key 体系。
- 向量检索接入。
- 场景 A 增强：多轮记忆、主动推荐、答案沉淀升级。

**v0.3**：

- MCP Server。
- API 配额限流和审计面板。
- Reranker 接入。
- Webhook：投喂完成、任务失败时主动通知外部系统。

## 28. v0.2 关键决策点

以下决策影响 v0.2 实施路径，需在动工前确认：

1. **向量库选型**：留在 MySQL（简单但能力有限）还是引入 Qdrant / pgvector（能力强但增加部署复杂度）。
2. **MCP Server 优先级**：v0.2 立刻做，还是 REST API 稳定后 v0.3 再做。
3. **多模态处理**：自建模型（Whisper 本地部署）还是调用云 API（通义千问 VL、OpenAI Whisper API）。
4. **API 鉴权**：仅 API Key（简单）还是 OAuth2（标准但复杂）。
5. **Webhook 必要性**：是否在 v0.2 提供，还是延后到 v0.3。

## 29. v0.2 一句话总结

把 Obsidian Vault 升级为对人和对 AI 双友好的知识源：人通过 Web Chat 使用，AI 通过 REST 或 MCP 调用，所有知识入口最终都沉淀为同一份可读、可追溯、可版本化的 Markdown。
