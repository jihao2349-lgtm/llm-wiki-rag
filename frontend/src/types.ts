export type PageKey =
  | "dashboard"
  | "sources"
  | "tasks"
  | "chat"
  | "wiki"
  | "embedding"
  | "apiconsole"
  | "settings"

export type IconName =
  | "alert"
  | "archive"
  | "audio"
  | "bot"
  | "check"
  | "code"
  | "copy"
  | "database"
  | "external"
  | "file"
  | "folder"
  | "globe"
  | "image"
  | "key"
  | "layout"
  | "link"
  | "list"
  | "message"
  | "mcp"
  | "play"
  | "plus"
  | "refresh"
  | "save"
  | "search"
  | "settings"
  | "spark"
  | "trash"
  | "upload"
  | "video"
  | "x"

export type Modality = "text" | "image" | "audio" | "video" | "mixed"

export type SourceStatus = "已摄入" | "解析中" | "待摄入" | "失败"

export type TaskStatus = "Pending" | "Processing" | "Done" | "Failed" | "Cancelled" | "ManualCheck"

export interface VaultProject {
  id?: number
  name: string
  path: string
  purpose: string
  health: "ready" | "needs-setup"
  lastIndexedAt: string
}

export interface Metric {
  label: string
  value: string
  description: string
  tone: "teal" | "blue" | "amber" | "red" | "violet"
  icon?: IconName
}

export interface SourceDocument {
  id: string
  title: string
  type: string
  modality: Modality
  status: SourceStatus
  originalPath: string
  extractedTextPath: string
  targetPage: string
  updatedAt: string
  summary: string
  size?: string
}

export interface SourcePreview {
  sourceId: string
  title: string
  extractedTextPath: string
  content: string
}

export interface IngestTask {
  taskId: string
  sourceTitle: string
  status: TaskStatus
  progress: number
  retryCount: number
  errorMessage?: string
  updatedAt: string
  writtenFiles: string[]
}

export interface WikiTreeNode {
  path: string
  title: string
  type: "file" | "directory"
  children?: WikiTreeNode[]
}

export interface WikiPage {
  path: string
  title: string
  type: "index" | "source" | "concept" | "entity" | "synthesis" | "question"
  modality?: Modality
  updatedAt: string
  frontmatter: string
  body: string
}

export interface ChatReference {
  id: number
  title: string
  path: string
  quote: string
}

export interface ChatSession {
  id: string
  title: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  role: "user" | "assistant"
  content: string
  references?: ChatReference[]
  pinned?: boolean
}

export interface ChatSuggestion {
  question: string
  reason: string
}

export interface LlmSettings {
  vaultId?: number
  provider: string
  baseUrl: string
  apiKey?: string
  apiKeyMasked?: string
  model: string
  maxContextSize: number
  temperature: number
  outputLanguage: "Chinese" | "English" | "Auto"
  embeddingEnabled: boolean
  embeddingBaseUrl?: string
  embeddingApiKey?: string
  embeddingApiKeyMasked?: string
  embeddingModel?: string
  embeddingDimension?: number
  embeddingBatchSize?: number
  vectorBackend: "none" | "pgvector" | "qdrant"
  rerankerEnabled: boolean
}

export interface EmbeddingFailedPage {
  pageId: number
  path: string
  title: string
  error: string
}

export interface EmbeddingStats {
  total: number
  success: number
  failed: number
  pending: number
  lastEmbeddedAt: string
  failedPages: EmbeddingFailedPage[]
}

export interface EmbeddingProgress {
  processing: boolean
  current: number
  total: number
}

export interface ApiKey {
  id: string
  name: string
  prefix: string
  scopes: string[]
  qpsLimit: number
  monthlyQuota: number
  used: number
  enabled: boolean
  expiresAt?: string
  createdAt: string
}

export interface ApiCallLog {
  id: string
  apiKeyName: string
  endpoint: string
  query: string
  latencyMs: number
  tokens: number
  status: "success" | "error" | "rate_limited"
  createdAt: string
}

export interface RagChunk {
  path: string
  title: string
  type: string
  score: number
  content: string
}
