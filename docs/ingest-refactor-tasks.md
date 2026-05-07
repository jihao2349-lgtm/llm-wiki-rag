# AI 摄取改造 — 执行任务清单

> 依据：`docs/ingest-refactor-plan.md`
> 执行规范：karpathy-guidelines（最小变更、外科手术式、可验证目标）
> 执行顺序：严格按编号，任务间存在依赖关系

---

## 前置假设（执行前确认）

- [ ] `AppSettingDO` 中已有或可新增 `output.language` 字段（任务六需读取）
- [ ] `purpose.md`、`schema.md` 在 vault 根目录是可选文件，不存在时为空字符串（现有 `readSafe` 已处理）
- [ ] `VaultFileService.writeStringAtomically` 支持自动创建父目录
- [ ] 不引入新的 Maven 依赖（所有实现用标准库）

---

## 任务一：修复 FileBlockParser — 行级状态机替换正则

**目标文件：** `backend/src/main/java/com/jihao/aiwiki/domain/ingest/pipeline/FileBlockParser.java`

**现状：** 第 20-23 行使用 `Pattern.DOTALL` 正则，存在 4 个已知缺陷（CRLF 失败、fence 内 marker 提前结束、block 截断、空路径无处理）。

### 新增数据结构

在同包下新建 `ParseResult.java`（record 类）：

```java
// domain/ingest/pipeline/ParseResult.java
public record ParseResult(List<FileBlock> blocks, List<String> warnings) {}
```

### 改造 FileBlockParser

**删除：** 第 20-23 行 `BLOCK_PATTERN` 字段，第 31-43 行 `parse()` 方法全部替换。

**新增两个静态 Pattern（类级别）：**

```java
private static final Pattern OPENER = Pattern.compile("^---FILE:\\s*(.+?)\\s*---\\s*$");
private static final Pattern CLOSER = Pattern.compile("^---END FILE---\\s*$");
```

**新增 `parseWithWarnings(String llmOutput) → ParseResult` 方法（主入口）：**

逻辑（行级状态机）：
1. `llmOutput.replace("\r\n", "\n")` 统一换行
2. 按 `\n` 分割为行数组，逐行遍历
3. 状态变量：`inBlock: boolean`、`currentPath: String`、`contentLines: List<String>`、`fenceDepth: int`
4. 每行逻辑：
   - 非 inBlock 时：匹配 OPENER → 记录 path，进入 inBlock
   - inBlock 时：
     - 计 fence 深度：行以 ` ``` ` 或 `~~~` 开头，`fenceDepth ^= 1`
     - `fenceDepth == 0` 且匹配 CLOSER → 退出 inBlock，封装 FileBlock，清空状态
     - 否则 → `contentLines.add(line)`
5. 遍历结束后 `inBlock == true` → 添加 `ParseWarning("未关闭的 FILE block: " + currentPath)`
6. path 为空时 → 添加 warning，跳过该 block

**保留兼容旧接口：** `parse(String llmOutput) → List<FileBlock>` 内部调用 `parseWithWarnings`，丢弃 warnings 返回 blocks（避免改动调用方）。

### 成功验证

修改 `FileBlockParserTest.java`（已存在），新增/覆盖以下测试用例：

| 用例 | 预期 |
|------|------|
| CRLF 输入 | 正常解析，content 无 `\r` |
| fence 内含 `---END FILE---` | 不提前结束，完整解析 |
| 未关闭 block | 返回 1 个 warning，blocks 不含该 block |
| 空路径 block | 返回 1 个 warning，blocks 不含该 block |
| 正常单/多 block | blocks 数量正确 |

```
验证指令：mvn test -pl backend -Dtest=FileBlockParserTest
```

---

## 任务二：新增 IngestContentSanitizer

**目标文件：** 新建 `backend/src/main/java/com/jihao/aiwiki/domain/ingest/pipeline/IngestContentSanitizer.java`

**现状：** 无此类，约 45% 页面 frontmatter 无法解析。

### 实现

`@Component` 类，单一公开方法：

```java
public String sanitize(String content)
```

**规则 1 — 外层 code fence 剥除：**
- 判断：`content.stripLeading()` 以 ` ```yaml`、` ```markdown`、` ``` ` 开头
- 操作：去掉第一行和最后的 ` ``` ` 行

**规则 2 — `frontmatter:` 前缀剥除：**
- 判断：`content.stripLeading()` 以 `frontmatter:` 开头（忽略大小写）
- 操作：删除该前缀行

**规则 3 — frontmatter 内 wikilink 列表修复：**
- 作用范围：仅 frontmatter 区域（`---` 到第二个 `---` 之间）
- 匹配：`related:\s+\[\[.+\]\]` 形式（inline 列表含 wikilink）
- 操作：将每个 `[[x]]` 包裹为 `"[[x]]"`，整体输出为 YAML inline array

**三个规则依次顺序执行，互不耦合。**

### 成功验证

新建 `IngestContentSanitizerTest.java`：

| 输入 | 预期输出 |
|------|---------|
| ` ```yaml\n---\ntype: entity\n---\n# Body\n``` ` | `---\ntype: entity\n---\n# Body` |
| `frontmatter:\n---\ntype: entity\n---` | `---\ntype: entity\n---` |
| `related: [[a]], [[b]]` in frontmatter | `related: ["[[a]]", "[[b]]"]` |
| 正常内容（无需清洗） | 原样返回 |

```
验证指令：mvn test -pl backend -Dtest=IngestContentSanitizerTest
```

---

## 任务三：新增 SourcesMerger

**目标文件：** 新建 `backend/src/main/java/com/jihao/aiwiki/domain/ingest/pipeline/SourcesMerger.java`

**现状：** 无此类，多文件摄取后来源历史丢失。

### 实现

`@Component` 类，四个方法：

**`parseSources(String content) → List<String>`**
- 提取 frontmatter 中 `sources:` 字段
- 支持两种格式：
  - inline：`sources: ["a.pdf", "b.pdf"]`
  - multi-line：`sources:\n  - a.pdf\n  - b.pdf`
- 文件名去掉引号和前后空格

**`mergeSourcesLists(List<String> existing, List<String> incoming) → List<String>`**
- 大小写不敏感去重，保留 existing 原有顺序，incoming 新增条目追加到末尾

**`writeSources(String content, List<String> sources) → String`**
- 替换 frontmatter 中 `sources:` 行，统一改为 inline 格式：`sources: ["a", "b"]`
- 不修改 frontmatter 外的内容

**`mergeSourcesIntoContent(String newContent, String existingContent) → String`（主入口）**
- `existingContent` 为 null 或空 → 直接返回 `newContent`
- 否则：`parseSources(existingContent)` + `parseSources(newContent)` → `mergeSourcesLists` → `writeSources(newContent, merged)`

### 成功验证

新建 `SourcesMergerTest.java`：

| 场景 | 预期 |
|------|------|
| existing=null | 返回 newContent 原样 |
| inline + inline 合并，有重叠 | 去重，顺序正确 |
| multi-line + inline 合并 | 统一转 inline 格式 |
| 大小写不敏感去重 | `Paper.pdf` 和 `paper.pdf` 视为同一个 |

```
验证指令：mvn test -pl backend -Dtest=SourcesMergerTest
```

---

## 任务四：重构 IngestPipeline 写入逻辑

**目标文件：** `backend/src/main/java/com/jihao/aiwiki/domain/ingest/pipeline/IngestPipeline.java`

**现状问题（精确定位）：**
- 第 96-105 行：校验失败全部抛异常（改为 warn-and-skip）
- 第 112-119 行：所有文件统一 `writeStringAtomically`（改为三分支）
- 第 122-124 行：`appendWikiLog` 重复写 log.md（删除，log 改由 LLM FILE block 生成）
- 第 223 行：`sourceText.substring(0, 8000)` 二次截断（删除）

### 改造步骤（外科手术式，逐点修改）

**Step 1 — 新增依赖注入（构造函数）**

在构造函数参数列表新增：
```java
IngestContentSanitizer contentSanitizer,
SourcesMerger sourcesMerger
```

**Step 2 — 替换校验+写入循环（第 96-120 行）**

删除原有校验 block（第 96-105 行），替换为：

```
List<String> warnings = new ArrayList<>();
for (FileBlock block : blocks) {
    if (context.isCancellationRequested()) return;

    // 清洗
    String cleanedContent = contentSanitizer.sanitize(block.getContent());

    // per-block 校验（warn-and-skip）
    String pathErr = frontmatterValidator.validatePath(block.getPath());
    String fmErr = frontmatterValidator.validateFrontmatter(cleanedContent);
    if (pathErr != null || fmErr != null) {
        warnings.add("[SKIP] " + block.getPath() + ": " + (pathErr != null ? pathErr : fmErr));
        continue;
    }

    // 三分支写入
    if ("wiki/log.md".equals(block.getPath())) {
        // log：追加写（不覆写）
        String existing = readSafe(vaultRoot, "wiki/log.md");
        fileService.writeStringAtomically(vaultRoot, "wiki/log.md", existing + "\n" + cleanedContent);
    } else if ("wiki/index.md".equals(block.getPath()) || "wiki/overview.md".equals(block.getPath())) {
        // listing 页：整体覆写
        fileService.writeStringAtomically(vaultRoot, block.getPath(), cleanedContent);
    } else {
        // 内容页：先合并 sources，再覆写
        String existing = readSafe(vaultRoot, block.getPath());
        String merged = sourcesMerger.mergeSourcesIntoContent(cleanedContent, existing.isEmpty() ? null : existing);
        fileService.writeStringAtomically(vaultRoot, block.getPath(), merged);
    }

    writtenFiles.add(block.getPath());
}
if (!warnings.isEmpty()) {
    log.warn("Ingest warnings: {}", warnings);
}
```

**Step 3 — 删除 `appendWikiLog` 调用（第 122-124 行）及 `appendWikiLog` 方法（第 227-235 行）**

原因：log.md 现在由 LLM 的 FILE block 生成，不再后置追加（避免双写）。

**Step 4 — 删除二次截断（第 223 行）**

将：
```java
sourceText.length() > 8000 ? sourceText.substring(0, 8000) : sourceText
```
改为：
```java
sourceText
```

### 成功验证

无单元测试（依赖 LlmClient），通过集成验证：
- [ ] 摄取单个文件，`wiki/log.md` 有新 entry，历史 entry 保留
- [ ] 摄取两次相同来源，概念页 `sources` 字段只有一条（无重复）
- [ ] 单个 block 校验失败时，其他 block 正常写入（warn 日志可见）
- [ ] `wiki/index.md` 被整体覆写，`wiki/log.md` 被追加

```
验证指令：mvn compile -pl backend（编译通过为最低要求）
```

---

## 任务五：重写 buildAnalyzePrompt

**目标文件：** `IngestPipeline.java`，第 175-197 行 `buildAnalyzePrompt` 方法。

**现状：** 要求输出 JSON，无结构引导，LLM 分析质量低。

### 改造

**整体替换** `buildAnalyzePrompt` 方法体，输出改为结构化 Markdown（非 JSON）：

新 prompt 模板（参数不变：`purpose, schema, index, sourceText, source`）：

```
You are analyzing a source document to prepare knowledge extraction.

[Purpose of this wiki]
{purpose}

[Wiki Schema]
{schema}

[Current Wiki Index]
{index}

[Folder Context]
Path: {source.originalPath}

[Source Document]
Title: {source.title}

{sourceText}

---

Analyze the document and respond in the following structured format (Markdown, not JSON):

## Key Entities
For each: Name | Type (person/org/tool) | Role in this document | Already in wiki index? (yes/no)

## Key Concepts
For each: Name | Short definition | Why important here | Already in wiki index? (yes/no)

## Main Arguments & Findings
For each: Core claim | Supporting evidence | Evidence strength (strong/moderate/weak)

## Connections to Existing Wiki
How this document relates to existing content (reinforces/challenges/extends which pages)

## Contradictions & Tensions
Conflicts with existing wiki; internal tensions in the document

## Recommendations
Which pages to create or update | Key content focus | Open questions worth flagging
```

**注意：** `analysisJson` 变量（第 78 行）重命名为 `analysisResult`（可选，不强制）。

### 成功验证

- [ ] 编译通过
- [ ] 摄取一个真实文档，Stage 1 的 LLM 响应包含 `## Key Entities` 等章节头（查日志）

---

## 任务六：重写 buildGeneratePrompt

**目标文件：** `IngestPipeline.java`，第 200-225 行 `buildGeneratePrompt` 方法。

**现状：** 缺少 frontmatter 规范、语言指令、REVIEW block、index/overview 维护说明。

### 改造

**整体替换** `buildGeneratePrompt` 方法，签名新增 `language` 参数：

```java
private String buildGeneratePrompt(String analysisResult, String sourceText,
                                    String sourceSlug, String today,
                                    String title, String language)
```

调用处（第 87 行）同步新增 `language` 参数读取，从 `AppSettingDO` 获取 `output.language`，默认值 `"follow source language"`。

**新 prompt 完整模板（五个部分）：**

**① 语言指令（首部）**
```
Language instruction: {language}. All generated wiki page content MUST be written in this language.
```

**② Frontmatter 格式规范**
```
FRONTMATTER RULES (mandatory):
- First line must be exactly `---`, never use ```yaml fences
- Each field on its own line; arrays use inline format: tags: [a, b]
- `related` uses slugs only, no wiki/ prefix, no .md suffix
- `sources` must include the current source filename
- Wikilinks [[...]] only in body, never in frontmatter values
```

**③ 必须生成的文件清单**
```
REQUIRED FILES:
1. wiki/sources/{sourceSlug}.md (MANDATORY — exact path)
2. wiki/entities/<slug>.md for each key entity
3. wiki/concepts/<slug>.md for each key concept
4. wiki/index.md — append new entries, preserve ALL existing entries
5. wiki/log.md — only the new entry: ## [{today}] ingest | {title}
6. wiki/overview.md — 2-5 paragraph overview of the entire wiki content
```

**④ REVIEW block 格式**
```
For items requiring human judgment, use REVIEW blocks (outside FILE blocks):

---REVIEW: contradiction | Title---
Description of the issue
OPTIONS: Create Page | Skip
PAGES: wiki/relevant-page.md
SEARCH: query1 | query2
---END REVIEW---

Types: contradiction / duplicate / missing-page / suggestion
```

**⑤ 输出格式约束 + 语言指令重复（尾部）**
```
OUTPUT RULES:
- First character must be `-` (start of ---FILE:)
- No preamble, summary, or analysis text outside FILE/REVIEW blocks
- Nothing between FILE blocks except REVIEW blocks

Analysis:
{analysisResult}

Source text:
{sourceText}

Language instruction (repeated): {language}. Generate ALL content in this language.
```

### 成功验证

- [ ] 编译通过
- [ ] 摄取文档后，LLM 输出第一个字符为 `-`（查日志）
- [ ] 生成的 `wiki/sources/<slug>.md` frontmatter 无 ` ```yaml ` 包裹

---

## 任务七：新增 ReviewBlockParser

**目标文件：** 新建 `backend/src/main/java/com/jihao/aiwiki/domain/ingest/pipeline/ReviewBlockParser.java`

**现状：** 无此功能，无法捕获 LLM 标注的需人工判断项目。

### 新增数据结构

同包新建 `ReviewItem.java`（record 类）：

```java
public record ReviewItem(
    String type,        // contradiction / duplicate / missing-page / suggestion
    String title,
    String description,
    List<String> options,
    List<String> pages,
    List<String> searches
) {}
```

### 实现 ReviewBlockParser

`@Component` 类，单一公开方法：

```java
public List<ReviewItem> parse(String llmOutput)
```

**实现逻辑：**
- Pattern：`---REVIEW:\s*(\w[\w-]*)\s*\|\s*(.+?)---(.*?)---END REVIEW---`（`DOTALL`）
  - REVIEW block 不存在 fence 嵌套问题，用正则即可
- 对每个 match：
  - group(1)=type，group(2)=title，group(3)=body
  - 从 body 中提取：
    - `OPTIONS:` 后面的 `|` 分割列表
    - `PAGES:` 后面的 `,` 或换行分割列表
    - `SEARCH:` 后面的 `|` 分割列表
    - 其余文本作为 description

### 接入 IngestPipeline

在 `IngestPipeline` 构造函数注入 `ReviewBlockParser`；`run()` 方法末尾（写入循环之后）：

```java
List<ReviewItem> reviews = reviewBlockParser.parse(llmOutput);
if (!reviews.isEmpty()) {
    log.info("Ingest REVIEW items ({}): {}", reviews.size(), reviews);
    // TODO: 后续版本入库，当前只记录日志
}
```

### 成功验证

新建 `ReviewBlockParserTest.java`：

| 用例 | 预期 |
|------|------|
| 含 1 个 contradiction block | 返回 1 个 ReviewItem，type="contradiction" |
| 含多个 REVIEW block | 全部解析 |
| 无 REVIEW block | 返回空列表 |
| type/title/options/pages/searches 各字段 | 解析正确 |

```
验证指令：mvn test -pl backend -Dtest=ReviewBlockParserTest
```

---

## 任务八：补充 source summary 兜底逻辑

**目标文件：** `IngestPipeline.java`，写入循环之后、进度更新之前。

**现状：** LLM 未生成 `wiki/sources/<slug>.md` 时无任何处理。

### 实现

在 `run()` 方法写入循环之后（第 125 行 `context.updateProgress` 之前）插入：

```java
boolean hasSourcePage = writtenFiles.stream()
        .anyMatch(p -> p.startsWith("wiki/sources/"));
if (!hasSourcePage) {
    String stubPath = "wiki/sources/" + sourceSlug + ".md";
    String stubContent = """
            ---
            type: source
            title: "Source: %s"
            created: %s
            updated: %s
            sources: ["%s"]
            tags: []
            related: []
            ---

            # Source: %s

            %s
            """.formatted(
                source.getTitle(), today, today,
                source.getOriginalPath() != null ? source.getOriginalPath() : sourceSlug,
                source.getTitle(),
                analysisResult.length() > 3000 ? analysisResult.substring(0, 3000) : analysisResult
            );
    fileService.writeStringAtomically(vaultRoot, stubPath, stubContent);
    writtenFiles.add(stubPath);
    log.info("Generated fallback source summary: {}", stubPath);
}
```

**注意：** `analysisResult`（原 `analysisJson`）在此处需可访问，确保变量作用域在 `run()` 方法级别。

### 成功验证

- [ ] 编译通过
- [ ] 用一个极简源文件摄取（不太可能生成 sources 页），验证 `wiki/sources/<slug>.md` 自动创建
- [ ] stub 的 frontmatter 格式合法（运行 `MarkdownFrontmatterValidator`）

---

## 执行检查清单

按执行顺序，每完成一个任务后勾选：

- [ ] **任务一** FileBlockParser 行级解析器 — `FileBlockParserTest` 全部通过
- [ ] **任务二** IngestContentSanitizer — `IngestContentSanitizerTest` 全部通过
- [ ] **任务三** SourcesMerger — `SourcesMergerTest` 全部通过
- [ ] **任务四** IngestPipeline 写入重构 — `mvn compile` 通过，集成验证 4 项
- [ ] **任务五** buildAnalyzePrompt 重写 — 编译通过，日志包含 `## Key Entities`
- [ ] **任务六** buildGeneratePrompt 重写 — 编译通过，输出首字符为 `-`
- [ ] **任务七** ReviewBlockParser — `ReviewBlockParserTest` 全部通过，日志可见
- [ ] **任务八** source summary 兜底 — stub 文件自动生成，frontmatter 合法

---

## 不在本次实现范围内

以下内容在需求文档中明确标注为"后续扩展预留"，**不实现，不预埋抽象**：

- `ChunkStrategy` 接口（大文档分块摄取）
- `ContentExtractor` 多模态扩展点（图片/视频/音频）
- 摄取缓存（SHA-256 哈希 + `content_hash` 字段）
- ReviewItem 入库（任务七当前仅记录日志）
