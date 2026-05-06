import type {
  ApiCallLog,
  ApiKey,
  ChatMessage,
  ChatReference,
  ChatSuggestion,
  IngestTask,
  LlmSettings,
  Metric,
  RagChunk,
  SourceDocument,
  VaultProject,
  WikiPage,
} from "./types"

export const vaultProject: VaultProject = {
  name: "Personal AI Notes",
  path: "/Users/jihao/Documents/Obsidian/Personal AI Notes",
  purpose: "把论文、网页、音视频和工作资料编译成可追踪的 Obsidian Markdown Wiki。",
  health: "ready",
  lastIndexedAt: "3 分钟前",
}

export const metrics: Metric[] = [
  { label: "原始资料", value: "128", description: "+12 本周", tone: "teal", icon: "archive" },
  { label: "Wiki 页面", value: "384", description: "42 个概念页", tone: "blue", icon: "file" },
  { label: "索引块", value: "2,916", description: "98% 已同步", tone: "violet", icon: "database" },
  { label: "待审核", value: "7", description: "3 个冲突", tone: "amber", icon: "alert" },
]

export const sources: SourceDocument[] = [
  {
    id: "src-001",
    title: "agent-memory-survey.pdf",
    type: "PDF",
    modality: "text",
    status: "已摄入",
    originalPath: "raw/sources/agent-memory-survey.pdf",
    extractedTextPath: ".ai-wiki/cache/agent-memory-survey.txt",
    targetPage: "wiki/sources/agent-memory-survey.md",
    updatedAt: "10 分钟前",
    size: "2.4 MB",
    summary: "综述了短期会话状态、长期语义记忆和 episodic traces 的组合方式。",
  },
  {
    id: "src-002",
    title: "LLM Wiki Methodology",
    type: "网页",
    modality: "text",
    status: "解析中",
    originalPath: "raw/sources/webclips/llm-wiki-methodology.html",
    extractedTextPath: ".ai-wiki/cache/llm-wiki-methodology.txt",
    targetPage: "wiki/sources/llm-wiki-methodology.md",
    updatedAt: "18 分钟前",
    size: "186 KB",
    summary: "网页正文已抓取，正在抽取标题、正文和原始链接。",
  },
  {
    id: "src-003",
    title: "knowledge-graph-talk.mp4",
    type: "视频",
    modality: "video",
    status: "解析中",
    originalPath: "raw/assets/knowledge-graph-talk.mp4",
    extractedTextPath: ".ai-wiki/cache/knowledge-graph-talk.txt",
    targetPage: "wiki/sources/knowledge-graph-talk.md",
    updatedAt: "25 分钟前",
    size: "412 MB",
    summary: "Whisper 已转写 38% 内容，关键帧 caption 待生成。",
  },
  {
    id: "src-004",
    title: "lecture-2026-04.mp3",
    type: "音频",
    modality: "audio",
    status: "已摄入",
    originalPath: "raw/assets/lecture-2026-04.mp3",
    extractedTextPath: ".ai-wiki/cache/lecture-2026-04.txt",
    targetPage: "wiki/sources/lecture-2026-04.md",
    updatedAt: "2 小时前",
    size: "78 MB",
    summary: "Whisper 转写完成，已按章节切分并生成时间戳索引。",
  },
  {
    id: "src-005",
    title: "system-architecture.png",
    type: "图片",
    modality: "image",
    status: "已摄入",
    originalPath: "raw/assets/system-architecture.png",
    extractedTextPath: ".ai-wiki/cache/system-architecture.txt",
    targetPage: "wiki/sources/system-architecture.md",
    updatedAt: "3 小时前",
    size: "1.1 MB",
    summary: "VLM 已生成架构图描述，OCR 抽取了节点标签。",
  },
  {
    id: "src-006",
    title: "personal-knowledge-base-notes.md",
    type: "Markdown",
    modality: "text",
    status: "待摄入",
    originalPath: "raw/sources/files/personal-knowledge-base-notes.md",
    extractedTextPath: ".ai-wiki/cache/personal-knowledge-base-notes.txt",
    targetPage: "wiki/sources/personal-knowledge-base-notes.md",
    updatedAt: "1 小时前",
    size: "24 KB",
    summary: "包含本地优先知识库、人工 review 和 schema 约束的设计笔记。",
  },
  {
    id: "src-007",
    title: "old-rag-notes.docx",
    type: "DOCX",
    modality: "text",
    status: "失败",
    originalPath: "raw/sources/files/old-rag-notes.docx",
    extractedTextPath: ".ai-wiki/cache/old-rag-notes.txt",
    targetPage: "wiki/sources/old-rag-notes.md",
    updatedAt: "昨天",
    size: "320 KB",
    summary: "模型输出缺少 END FILE 标记，原始文件已保留。",
  },
]

export const tasks: IngestTask[] = [
  {
    taskId: "task-20260506-001",
    sourceTitle: "agent-memory-survey.pdf",
    status: "Processing",
    progress: 68,
    retryCount: 0,
    updatedAt: "刚刚",
    writtenFiles: ["wiki/sources/agent-memory-survey.md"],
  },
  {
    taskId: "task-20260506-002",
    sourceTitle: "knowledge-graph-talk.mp4",
    status: "Processing",
    progress: 38,
    retryCount: 0,
    updatedAt: "5 分钟前",
    writtenFiles: [],
  },
  {
    taskId: "task-20260506-003",
    sourceTitle: "LLM Wiki Methodology",
    status: "Pending",
    progress: 0,
    retryCount: 0,
    updatedAt: "18 分钟前",
    writtenFiles: [],
  },
  {
    taskId: "task-20260505-014",
    sourceTitle: "old-rag-notes.docx",
    status: "Failed",
    progress: 42,
    retryCount: 2,
    errorMessage: "模型输出缺少 END FILE 标记，已阻止写入半成品。",
    updatedAt: "昨天",
    writtenFiles: [],
  },
  {
    taskId: "task-20260505-011",
    sourceTitle: "lecture-2026-04.mp3",
    status: "Done",
    progress: 100,
    retryCount: 0,
    updatedAt: "2 小时前",
    writtenFiles: ["wiki/sources/lecture-2026-04.md", "wiki/log.md"],
  },
]

export const wikiPages: WikiPage[] = [
  {
    path: "wiki/index.md",
    title: "Wiki Index",
    type: "index",
    updatedAt: "3 分钟前",
    frontmatter: "---\ntype: index\ntitle: Wiki Index\nupdated: 2026-05-06\n---",
    body: "# Wiki Index\n\n- [[agent-memory]]\n- [[local-first-ai-memory]]\n- [[obsidian]]\n- [[knowledge-graph-talk]]",
  },
  {
    path: "wiki/sources/agent-memory-survey.md",
    title: "Agent Memory Survey",
    type: "source",
    modality: "text",
    updatedAt: "10 分钟前",
    frontmatter:
      "---\ntype: source\ntitle: Agent Memory Survey\nmodality: text\nsources:\n  - raw/sources/agent-memory-survey.pdf\nrelated:\n  - agent-memory\n---",
    body:
      "# Agent Memory Survey\n\n这份资料将 Agent Memory 分成短期会话状态、长期语义记忆和可追溯情节记录。\n\n## 关键观点\n\n- 长期记忆需要可审计和可恢复。\n- 写入知识库前需要 schema 与 review queue 约束。",
  },
  {
    path: "wiki/concepts/agent-memory.md",
    title: "Agent Memory",
    type: "concept",
    modality: "text",
    updatedAt: "10 分钟前",
    frontmatter:
      "---\ntype: concept\ntitle: Agent Memory\nsources:\n  - wiki/sources/agent-memory-survey.md\nrelated:\n  - local-first-ai-memory\n---",
    body:
      "# Agent Memory\n\nAgent Memory 是智能体在多轮任务中保存、检索和更新上下文的能力。个人知识库场景中，长期记忆应落在可审计的 Markdown 页面，而不是隐藏在不可见的向量数据库里。\n\n## 相关页面\n\n- [[local-first-ai-memory]]\n- [[llm-wiki]]\n- [[obsidian]]",
  },
  {
    path: "wiki/sources/knowledge-graph-talk.md",
    title: "Knowledge Graph Talk",
    type: "source",
    modality: "video",
    updatedAt: "5 分钟前",
    frontmatter:
      "---\ntype: source\ntitle: Knowledge Graph Talk\nmodality: video\nasset_refs:\n  - raw/assets/knowledge-graph-talk.mp4\nextraction_method: whisper-large-v3\n---",
    body:
      "# Knowledge Graph Talk\n\n## [00:00 - 04:32] 引言\n知识图谱构建的核心挑战在于实体消歧和关系抽取。\n\n## [04:33 - 12:18] 实体抽取\n对比了基于规则、基于统计和基于 LLM 的方法。",
  },
  {
    path: "wiki/synthesis/local-first-ai-memory.md",
    title: "Local-first AI Memory",
    type: "synthesis",
    modality: "text",
    updatedAt: "昨天",
    frontmatter:
      "---\ntype: synthesis\ntitle: Local-first AI Memory\nsources:\n  - wiki/concepts/agent-memory.md\n---",
    body:
      "# Local-first AI Memory\n\n本地优先的 AI 记忆系统应把长期知识写入用户可编辑、可同步、可备份的目录，并保留原始资料和生成记录。",
  },
]

export const references: ChatReference[] = [
  {
    id: 1,
    title: "Agent Memory",
    path: "wiki/concepts/agent-memory.md",
    quote: "长期记忆应落在可审计的 Markdown 页面。",
  },
  {
    id: 2,
    title: "Agent Memory Survey",
    path: "wiki/sources/agent-memory-survey.md",
    quote: "Agent Memory 分成短期会话状态、长期语义记忆和情节记录。",
  },
  {
    id: 3,
    title: "Local-first AI Memory",
    path: "wiki/synthesis/local-first-ai-memory.md",
    quote: "长期知识写入用户可编辑、可同步、可备份的目录。",
  },
]

export const chatMessages: ChatMessage[] = [
  {
    id: "msg-1",
    role: "user",
    content: "这些资料里关于 Agent Memory 的观点有哪些？",
  },
  {
    id: "msg-2",
    role: "assistant",
    content:
      "当前资料把 Agent Memory 分成三层：短期会话状态、长期语义记忆和可追溯的情节记录。对个人知识库场景，更稳定的做法是把长期记忆落到 Obsidian Markdown，并通过 schema、FILE block 校验和 review queue 控制写入风险。",
    references,
    pinned: true,
  },
]

export const chatSuggestions: ChatSuggestion[] = [
  {
    question: "Agent Memory 与 RAG 的边界在哪里？",
    reason: "已检索到相关概念，但未找到对比分析。",
  },
  {
    question: "Vault 中关于 review queue 的具体规则？",
    reason: "提及但未独立成页。",
  },
  {
    question: "对比当前几种向量库的取舍？",
    reason: "技术方案中提到但缺乏对比页面。",
  },
]

export const llmSettings: LlmSettings = {
  provider: "OpenAI-compatible",
  baseUrl: "http://localhost:11434/v1",
  model: "qwen3:14b",
  maxContextSize: 32000,
  temperature: 0.2,
  outputLanguage: "Chinese",
  embeddingEnabled: false,
  vectorBackend: "none",
  rerankerEnabled: false,
}

export const apiKeys: ApiKey[] = [
  {
    id: "key-001",
    name: "Claude Code MCP",
    prefix: "sk-aw-7f3c",
    scopes: ["search", "read", "list"],
    qpsLimit: 10,
    monthlyQuota: 50000,
    used: 12480,
    enabled: true,
    createdAt: "2026-04-28",
  },
  {
    id: "key-002",
    name: "Personal Bot",
    prefix: "sk-aw-a92e",
    scopes: ["search", "answer", "read"],
    qpsLimit: 5,
    monthlyQuota: 20000,
    used: 3210,
    enabled: true,
    expiresAt: "2026-08-01",
    createdAt: "2026-05-01",
  },
  {
    id: "key-003",
    name: "Legacy Script",
    prefix: "sk-aw-1b0d",
    scopes: ["search"],
    qpsLimit: 2,
    monthlyQuota: 5000,
    used: 4980,
    enabled: false,
    createdAt: "2026-03-15",
  },
]

export const apiCallLogs: ApiCallLog[] = [
  {
    id: "log-001",
    apiKeyName: "Claude Code MCP",
    endpoint: "POST /api/v1/rag/search",
    query: "Agent Memory 设计模式",
    latencyMs: 142,
    tokens: 1820,
    status: "success",
    createdAt: "2 分钟前",
  },
  {
    id: "log-002",
    apiKeyName: "Personal Bot",
    endpoint: "POST /api/v1/rag/answer",
    query: "本地优先的知识库有哪些方案？",
    latencyMs: 2840,
    tokens: 3580,
    status: "success",
    createdAt: "8 分钟前",
  },
  {
    id: "log-003",
    apiKeyName: "Claude Code MCP",
    endpoint: "GET /api/v1/rag/page",
    query: "wiki/concepts/agent-memory.md",
    latencyMs: 38,
    tokens: 0,
    status: "success",
    createdAt: "15 分钟前",
  },
  {
    id: "log-004",
    apiKeyName: "Legacy Script",
    endpoint: "POST /api/v1/rag/search",
    query: "向量检索方案对比",
    latencyMs: 12,
    tokens: 0,
    status: "rate_limited",
    createdAt: "32 分钟前",
  },
  {
    id: "log-005",
    apiKeyName: "Personal Bot",
    endpoint: "POST /api/v1/rag/ingest",
    query: "external://blog.example.com/post-2026-05",
    latencyMs: 4120,
    tokens: 0,
    status: "success",
    createdAt: "1 小时前",
  },
]

export const ragSampleChunks: RagChunk[] = [
  {
    path: "wiki/concepts/agent-memory.md",
    title: "Agent Memory",
    type: "concept",
    score: 0.87,
    content: "Agent Memory 通常分为三层：短期会话状态、长期语义记忆和可追溯情节记录。",
  },
  {
    path: "wiki/sources/agent-memory-survey.md",
    title: "Agent Memory Survey",
    type: "source",
    score: 0.74,
    content: "综述将 Agent Memory 设计模式归为 buffer、summary、retrieval 三类。",
  },
  {
    path: "wiki/synthesis/local-first-ai-memory.md",
    title: "Local-first AI Memory",
    type: "synthesis",
    score: 0.62,
    content: "本地优先记忆要求所有持久化数据可读、可编辑、可备份。",
  },
]

export const mcpConfigSample = `{
  "mcpServers": {
    "ai-wiki": {
      "command": "npx",
      "args": ["-y", "@ai-wiki/mcp-server"],
      "env": {
        "AI_WIKI_BASE_URL": "http://localhost:8080",
        "AI_WIKI_API_KEY": "sk-aw-7f3c..."
      }
    }
  }
}`
