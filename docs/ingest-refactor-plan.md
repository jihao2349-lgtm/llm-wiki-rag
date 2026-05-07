# AI 摄取改造计划

> 参考项目：`llm_wiki`（TypeScript/Tauri 桌面端）
> 改造目标：`backend`（Java/Spring Boot 服务端）
> 目标：对齐参考项目的 AI 摄取逻辑，提升生成质量与数据正确性

---

## 一、AI 摄取的核心逻辑理解

### 1.1 摄取的本质是"知识提炼"，不是"文档拆分"

将原始文件（PDF / DOCX / PPTX / URL 等）经过 LLM 理解后，**重新组织**为结构化知识库，而非简单切割文件。

例如一篇论文摄取后会生成：
- `wiki/sources/paper.md` — 这篇论文的摘要页
- `wiki/entities/vaswani.md` — 论文中提到的关键人物/组织
- `wiki/concepts/transformer.md` — 论文中的核心概念/方法
- `wiki/index.md` — 全库目录（更新）
- `wiki/overview.md` — 全库综合描述（更新）

### 1.2 完整摄取流程

```
源文件 (PDF/DOCX/PPTX/URL...)
    ↓ 解析器提取纯文本
LLM Stage 1（分析）：理解实体、概念、论点、与已有知识的关联
    ↓ 分析结果作为 context
LLM Stage 2（生成）：输出 FILE blocks（多个 Markdown 文件的内容）
    ↓ 解析 FILE blocks
按类型写入 wiki/ 目录（log 追加 / index 覆写 / 内容页合并 sources）
    ↓
向量嵌入 → 写入 LanceDB（RAG 检索索引）
```

### 1.3 FILE blocks 是什么

LLM 不能直接操作文件系统，所以用约定的文本协议来"表达写文件的意图"，后端负责真正执行：

```
---FILE: wiki/concepts/transformer.md---
---
type: concept
title: Transformer
sources: ["attention-paper.pdf"]
---

# Transformer

Transformer 是一种基于自注意力机制的神经网络架构...
---END FILE---
```

### 1.4 原始文件的定位

```
raw/sources/paper.pdf        ← 原始文件，永久保留，不参与 RAG 检索
wiki/sources/paper.md        ← 摄取后的摘要页，参与 RAG
wiki/concepts/transformer.md ← 提炼出的概念页，参与 RAG
```

原始文件通过 wiki 页面 frontmatter 的 `sources` 字段追踪，用于来源溯源、重新摄取、用户查看原文。

### 1.5 用户提问时的知识检索逻辑（RAG）

```
用户提问
    ↓
① 向量检索（语义相似，基于 embedding）
② 关键词检索（精确匹配，BM25）
    ↓
RRF（Reciprocal Rank Fusion）融合排序 → Top-K wiki 页面
    ↓
组装 prompt：overview.md（全局描述）+ Top-K 页面内容 + 用户问题
    ↓
LLM 生成回答（附来源：wiki 页面 → 原始文件）
```

### 1.6 wiki/ 目录结构设计意图

```
wiki/
├── index.md          # 全库目录（Stage 1 分析时 LLM 读取，避免重复创建页面）
├── overview.md       # 全库综合描述（Chat 时作为系统 context）
├── log.md            # 操作日志（追加写，不覆写）
├── sources/          # 每个原始文档对应一个摘要页
├── entities/         # 人、组织、工具（跨文档复用，sources 字段汇聚来源）
└── concepts/         # 理论、方法、算法（核心知识单元）
```

设计原因：
- `sources/` 和 `concepts/` 分开 — 不同知识粒度，RAG 时可精准过滤
- `entities/` 单独 — 跨文档复用率高，sources 合并后保留完整来源历史
- `index.md` — Stage 1 的输入，让 LLM 知道现有内容避免重复
- `overview.md` — Chat 的全局 context，让 LLM 理解知识库整体主题

### 1.7 大文档的处理思路（待实现）

当前方案截断到 50000 字符，超出部分丢弃，对大论文是有损的。更完整的思路：

```
方案 A：分块摄取
大文档 → 按 chunk 分段 → 每段单独摄取 → 结果合并（entity/concept 去重）

方案 B：分层摄取
大文档 → LLM 先提取目录结构 → 按章节分段摄取

方案 C：滚动窗口
固定窗口大小 + overlap，确保跨块内容不被截断
```

当前版本先不实现，但架构上在 `IngestPipeline` 中预留 `chunkStrategy` 扩展点。

---

## 二、现状问题分析

### 当前 backend 实现的主要缺陷

| 问题 | 位置 | 后果 |
|------|------|------|
| FileBlockParser 用正则匹配 | `FileBlockParser.java` | CRLF、fence 内 marker、截断 block 全部解析失败 |
| 无 LLM 输出清洗 | `IngestPipeline.java` | ~45% 页面 frontmatter 无法解析 |
| 无 sources 字段合并 | `IngestPipeline.java` | 多文件摄取后来源历史丢失，误触发删除 |
| 所有文件统一覆写 | `IngestPipeline.java` | log.md 历史被覆盖；index.md 合并有风险 |
| 校验失败全部抛异常 | `IngestPipeline.java` | 一个 block 问题导致整批文件不写 |
| 分析 Prompt 无结构引导 | `IngestPipeline.java` | LLM 分析质量低，生成页面质量差 |
| 生成 Prompt 缺失关键规则 | `IngestPipeline.java` | 无语言指令、无 REVIEW block、无 frontmatter 格式规范 |
| 源文本二次截断到 8000 字符 | `IngestPipeline.java` | 中等论文内容严重丢失 |
| 无 source summary 兜底 | `IngestPipeline.java` | LLM 未生成摘要页时无任何处理 |
| 无 REVIEW block 支持 | 整个 pipeline | 矛盾/重复/缺失页面无反馈机制 |

---

## 三、改造任务清单

### 任务一：修复 FileBlockParser — 行级解析器替换正则

**文件：** `domain/ingest/pipeline/FileBlockParser.java`

**现状：** 用 `Pattern.DOTALL` 正则，4 个已知缺陷。

**目标：** 行级状态机解析，对应参考实现 `parseFileBlocks()`。

改造点：
- 按行遍历，`OPENER_LINE` / `CLOSER_LINE` 两个正则做状态切换
- 跟踪 fence 状态（` ``` ` / `~~~`），fence 内的 closer 不视为结束
- CRLF 统一转 LF
- 未关闭 block → `ParseWarning`（不抛异常，记录后继续）
- 空 path block → `ParseWarning`
- 结果封装为 `ParseResult { List<FileBlock> blocks, List<String> warnings }`

---

### 任务二：新增 IngestContentSanitizer — LLM 输出清洗

**文件：** 新建 `domain/ingest/pipeline/IngestContentSanitizer.java`

**现状：** 无此步骤，对应参考实现 `sanitizeIngestedFileContent()`。

三个清洗规则：

**规则 1：外层 code fence 剥除**
```
输入：
```yaml
---
type: entity
---
# Body
```
输出：
---
type: entity
---
# Body
```

**规则 2：`frontmatter:` 前缀剥除**
```
输入：
frontmatter:
---
type: entity
---
输出：
---
type: entity
---
```

**规则 3：frontmatter 内 wikilink 列表修复**
```
输入：related: [[a]], [[b]], [[c]]
输出：related: ["[[a]]", "[[b]]", "[[c]]"]
```

---

### 任务三：新增 SourcesMerger — sources 字段合并

**文件：** 新建 `domain/ingest/pipeline/SourcesMerger.java`

**现状：** 无此步骤，对应参考实现 `mergeSourcesIntoContent()`。

核心方法：
- `parseSources(String content)` — 支持 inline `["a","b"]` 和 multi-line `- a` 格式
- `mergeSourcesLists(List<String> existing, List<String> incoming)` — 大小写不敏感去重，保留原有顺序
- `writeSources(String content, List<String> sources)` — 替换 frontmatter 内 sources 行，统一为 inline 格式
- `mergeSourcesIntoContent(String newContent, String existingContent)` — 主入口，existing 为 null 时直接返回 new

---

### 任务四：重构 IngestPipeline 写入逻辑 — 按文件类型区分

**文件：** `domain/ingest/pipeline/IngestPipeline.java`

**现状：** 所有文件统一 `writeStringAtomically`，校验失败全部抛异常。

写入策略改为三分支：

```
wiki/log.md                         → 读旧内容，追加新 entry（不覆写）
wiki/index.md / wiki/overview.md    → 整体覆写（listing 页，合并无意义）
其他内容页（entities/concepts/sources） → 先 SourcesMerger 合并 sources，再覆写
```

同时改造：
- 每个 block 写入前调用 `IngestContentSanitizer` 清洗
- 校验失败改为 per-block warn-and-skip，收集 `List<String> warnings`
- 移除 `buildGeneratePrompt` 里 `sourceText.substring(0, 8000)` 的二次截断

---

### 任务五：重写 buildAnalyzePrompt — 结构化分析引导

**文件：** `domain/ingest/pipeline/IngestPipeline.java`

**现状：** 要求输出 JSON，无结构引导，对应参考实现 `buildAnalysisPrompt()`。

新 prompt 结构（Markdown 格式输出，非 JSON）：

```
## Key Entities
每个实体：名字 + 类型 + 在本文中的角色 + 是否已在 wiki index 中存在

## Key Concepts
每个概念：名字 + 简短定义 + 为何在此文中重要 + 是否已存在

## Main Arguments & Findings
核心主张/结论 + 支撑证据 + 证据强度

## Connections to Existing Wiki
本文如何关联现有内容（强化/挑战/扩展）

## Contradictions & Tensions
与已有 wiki 的冲突点；内部张力

## Recommendations
建议创建/更新哪些页 + 重点内容 + 值得标记的开放问题
```

同时注入：`purpose.md`、`schema.md`、`wiki/index.md`、folder context（文件夹路径层次）。

---

### 任务六：重写 buildGeneratePrompt — 完整生成指令

**文件：** `domain/ingest/pipeline/IngestPipeline.java`

**现状：** 缺少 frontmatter 规范、语言指令、REVIEW block、index/overview 维护说明。

新 prompt 包含以下部分：

**① Frontmatter 格式规范（强制）**
```
- 第一行必须是 `---`，禁止用 ```yaml 围栏包裹
- 每个字段独立一行，数组用 inline 格式：tags: [a, b]
- related 用 slug，不含 wiki/ 或 .md 后缀
- sources 必须包含当前源文件名
- wikilink 语法 [[...]] 只用于 body，不用于 frontmatter
```

**② 必须生成的文件清单**
```
1. wiki/sources/<slug>.md（必须生成，路径精确）
2. wiki/entities/ — 关键实体页
3. wiki/concepts/ — 关键概念页
4. wiki/index.md — 在现有条目基础上追加，保留所有已有条目
5. wiki/log.md — 只输出追加的新 entry（格式：## [YYYY-MM-DD] ingest | Title）
6. wiki/overview.md — 更新为反映整个 wiki 内容的 2-5 段总览
```

**③ REVIEW block 格式**（需人工判断的项目）
```
---REVIEW: type | Title---
description
OPTIONS: Create Page | Skip
PAGES: wiki/page1.md
SEARCH: query1 | query2
---END REVIEW---

类型：contradiction / duplicate / missing-page / suggestion
```

**④ 输出格式约束**
```
- 第一个字符必须是 `-`（---FILE: 的开头）
- 不输出任何前言、摘要、分析文字
- FILE block 之外不输出任何内容
- 语言指令在 prompt 首尾各重复一次（防止模型忽略）
```

**⑤ 语言指令**

从 `AppSettingDO` 读取 `output.language` 配置注入，默认跟随源文件语言。

---

### 任务七：新增 ReviewBlockParser — 解析 REVIEW block

**文件：** 新建 `domain/ingest/pipeline/ReviewBlockParser.java`

**现状：** 无此功能，对应参考实现 `parseReviewBlocks()`。

实现：
- 正则提取 `---REVIEW: type | Title---...---END REVIEW---`
- 解析 `OPTIONS:`、`PAGES:`、`SEARCH:` 字段
- type 映射：`contradiction / duplicate / missing-page / suggestion`
- 返回 `List<ReviewItem>`，在 `IngestPipeline.run()` 末尾批量入库

---

### 任务八：补充 source summary 兜底逻辑

**文件：** `domain/ingest/pipeline/IngestPipeline.java`

**现状：** LLM 未生成 `wiki/sources/<slug>.md` 时无任何处理。

当写入文件列表中没有任何 `wiki/sources/` 路径时，自动生成 stub：

```markdown
---
type: source
title: "Source: <fileName>"
created: <today>
updated: <today>
sources: ["<fileName>"]
tags: []
related: []
---

# Source: <fileName>

<Stage 1 分析文本，前 3000 字符>
```

---

## 四、执行顺序

```
[基础设施层] 先完成，影响数据正确性
任务一（FileBlockParser 行级解析）
    ↓
任务二（IngestContentSanitizer 清洗）
    ↓
任务三（SourcesMerger sources 合并）
    ↓
任务四（写入逻辑重构：三分支 + 清洗 + warn-and-skip）

[Prompt 工程层] 基础设施稳定后推进
任务五（分析 Prompt 结构化）← 可与任务六并行
任务六（生成 Prompt 完整指令）

[功能补全层] 最后
任务七（ReviewBlockParser）
任务八（source summary 兜底）
```

---

## 五、后续扩展预留（当前版本不实现）

### 大文档分块摄取

在 `IngestPipeline` 中预留 `ChunkStrategy` 接口：

```java
interface ChunkStrategy {
    List<String> chunk(String text, int maxChunkSize);
}
// 默认实现：SimpleChunkStrategy（按字符截断）
// 后续实现：SemanticChunkStrategy（按段落/章节）
```

### 多模态支持（图片/视频/音频）

摄取流程中预留 `ContentExtractor` 扩展点，当前只有文本提取器，后续加：
- `ImageCaptionExtractor` — 视觉模型生成图片描述
- `AudioTranscriptExtractor` — Whisper 转录音频
- `VideoFrameExtractor` — 视频关键帧提取

### 摄取缓存

参考实现使用 SHA-256 对源文件内容做哈希，命中缓存时跳过 LLM 调用。
当前 backend 有 `IngestTask` 状态管理，可在此基础上加 `content_hash` 字段实现相同效果。
