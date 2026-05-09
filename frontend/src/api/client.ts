export function generateUUID(): string {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16)
  })
}

import type {
  ChatMessage,
  ChatReference,
  ChatSession,
  EmbeddingProgress,
  EmbeddingPageStatus,
  EmbeddingStats,
  IngestTask,
  LlmSettings,
  Metric,
  Modality,
  SourceDocument,
  SourcePreview,
  SourceStatus,
  TaskStatus,
  VaultProject,
  WikiPage,
  WikiTreeNode,
} from "../types"

const DEFAULT_API_BASE_URL = "http://localhost:8080"

export const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL
).replace(/\/$/, "")

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface PageResult<T> {
  records?: T[]
  list?: T[]
  rows?: T[]
  total?: number
}

export interface DashboardOverview {
  vaultProject: VaultProject
  metrics: Metric[]
  recentSources: SourceDocument[]
  activeTask?: IngestTask
}

export interface SourcePage {
  records: SourceDocument[]
  total: number
}

export interface TaskPage {
  records: IngestTask[]
  total: number
}

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message)
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null
}

function stringValue(value: unknown, fallback = "") {
  return typeof value === "string" ? value : fallback
}

function numberValue(value: unknown, fallback = 0) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback
}

function boolValue(value: unknown, fallback = false) {
  return typeof value === "boolean" ? value : fallback
}

function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : []
}

function pageRecords<T>(value: unknown, mapper: (item: unknown) => T): { records: T[]; total: number } {
  if (Array.isArray(value)) return { records: value.map(mapper), total: value.length }
  if (!isRecord(value)) return { records: [], total: 0 }

  const page = value as PageResult<unknown>
  const records = page.records ?? page.list ?? page.rows ?? []
  return {
    records: Array.isArray(records) ? records.map(mapper) : [],
    total: numberValue(page.total, Array.isArray(records) ? records.length : 0),
  }
}

function buildUrl(path: string, query?: Record<string, string | number | boolean | undefined>) {
  const url = new URL(`${apiBaseUrl}${path}`)
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== "") url.searchParams.set(key, String(value))
  })
  return url.toString()
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  query?: Record<string, string | number | boolean | undefined>,
): Promise<T> {
  let response: Response

  try {
    response = await fetch(buildUrl(path, query), {
      headers: {
        Accept: "application/json",
        ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
        ...options.headers,
      },
      ...options,
    })
  } catch {
    throw new ApiError(`无法连接后端 API：${apiBaseUrl}`)
  }

  const text = await response.text()
  const payload = text ? (JSON.parse(text) as unknown) : undefined

  if (!response.ok) {
    const message = isRecord(payload) ? stringValue(payload.message, response.statusText) : response.statusText
    throw new ApiError(message, response.status)
  }

  if (isRecord(payload) && "code" in payload && "data" in payload) {
    const apiResponse = payload as unknown as ApiResponse<T>
    if (apiResponse.code !== 200) throw new ApiError(apiResponse.message || "API 请求失败")
    return apiResponse.data
  }

  return payload as T
}

function inferModality(type: string, path: string): Modality {
  const value = `${type} ${path}`.toLowerCase()
  if (/\.(png|jpg|jpeg|webp|gif|svg)\b/.test(value)) return "image"
  if (/\.(mp3|wav|m4a|flac)\b/.test(value)) return "audio"
  if (/\.(mp4|mov|avi|mkv)\b/.test(value)) return "video"
  return "text"
}

function mapSourceStatus(value: string): SourceStatus {
  const normalized = value.toUpperCase()
  if (["DONE", "INGESTED", "PARSED"].includes(normalized)) return "已摄入"
  if (["PARSING", "PROCESSING", "UPLOADING"].includes(normalized)) return "解析中"
  if (["FAILED", "ERROR"].includes(normalized)) return "失败"
  return "待摄入"
}

function mapTaskStatus(value: string): TaskStatus {
  const normalized = value.toUpperCase()
  if (normalized === "PROCESSING" || normalized === "RUNNING") return "Processing"
  if (normalized === "DONE" || normalized === "SUCCESS") return "Done"
  if (normalized === "FAILED" || normalized === "ERROR") return "Failed"
  if (normalized === "CANCELLED" || normalized === "CANCELED") return "Cancelled"
  if (normalized === "MANUAL_CHECK" || normalized === "MANUALCHECK") return "ManualCheck"
  return "Pending"
}

function mapSourceDocument(item: unknown): SourceDocument {
  const row = isRecord(item) ? item : {}
  const id = String(row.id ?? row.sourceId ?? "")
  const type = stringValue(row.type, "Markdown")
  const originalPath = stringValue(row.originalPath)
  const title = stringValue(row.title, originalPath || id || "未命名资料")

  return {
    id,
    title,
    type,
    modality: inferModality(type, originalPath),
    status: mapSourceStatus(stringValue(row.status, "PENDING")),
    originalPath,
    extractedTextPath: stringValue(row.extractedTextPath),
    targetPage: stringValue(row.targetPage, `wiki/sources/${title}.md`),
    updatedAt: stringValue(row.updatedAt ?? row.updateTime ?? row.createTime, "未知"),
    summary: stringValue(row.summary ?? row.errorMessage, "暂无解析摘要"),
    size: stringValue(row.size),
  }
}

function mapTask(item: unknown): IngestTask {
  const row = isRecord(item) ? item : {}
  const rawWrittenFiles = row.writtenFiles ?? row.writtenFilesJson

  return {
    taskId: stringValue(row.taskId, String(row.id ?? "")),
    sourceTitle: stringValue(row.sourceTitle ?? row.title, `Source #${String(row.sourceId ?? "")}`),
    status: mapTaskStatus(stringValue(row.status, "PENDING")),
    progress: numberValue(row.progress),
    retryCount: numberValue(row.retryCount),
    errorMessage: stringValue(row.errorMessage),
    updatedAt: stringValue(row.updatedAt ?? row.updateTime ?? row.finishedAt ?? row.createTime, "未知"),
    writtenFiles: Array.isArray(rawWrittenFiles)
      ? stringArray(rawWrittenFiles)
      : stringArray(isRecord(row) ? row.written_files : undefined),
  }
}

function mapWikiType(value: string): WikiPage["type"] {
  if (["index", "source", "concept", "entity", "synthesis", "question"].includes(value)) {
    return value as WikiPage["type"]
  }
  return "source"
}

function mapWikiPage(item: unknown): WikiPage {
  const row = isRecord(item) ? item : {}
  const path = stringValue(row.path)
  const frontmatter = row.frontmatter

  return {
    path,
    title: stringValue(row.title, path || "未命名页面"),
    type: mapWikiType(stringValue(row.type, "source")),
    modality: inferModality(stringValue(row.modality), path),
    updatedAt: stringValue(row.updatedAt ?? row.updateTime, "未知"),
    frontmatter:
      typeof frontmatter === "string" ? frontmatter : JSON.stringify(frontmatter ?? {}, null, 2),
    body: stringValue(row.body ?? row.content),
  }
}

function mapTreeNode(item: unknown): WikiTreeNode {
  const row = isRecord(item) ? item : {}
  const children = Array.isArray(row.children) ? row.children.map(mapTreeNode) : undefined
  const isDir = row.directory === true || stringValue(row.type) === "directory"
  return {
    path: stringValue(row.path),
    title: stringValue(row.title ?? row.name ?? row.path, "未命名"),
    type: isDir ? "directory" : "file",
    children,
  }
}

function mapReference(item: unknown): ChatReference {
  const row = isRecord(item) ? item : {}
  return {
    id: numberValue(row.id, 0),
    title: stringValue(row.title, stringValue(row.path, "引用")),
    path: stringValue(row.path),
    quote: stringValue(row.quote ?? row.snippet ?? row.content),
  }
}

function mapChatMessage(item: unknown): ChatMessage {
  const row = isRecord(item) ? item : {}
  return {
    id: String(row.id ?? row.messageId ?? generateUUID()),
    role: stringValue(row.role) === "assistant" ? "assistant" : "user",
    content: stringValue(row.content),
    references: Array.isArray(row.references) ? row.references.map(mapReference) : undefined,
  }
}

function mapSession(item: unknown): ChatSession {
  const row = isRecord(item) ? item : {}
  return {
    id: String(row.id ?? row.sessionId ?? ""),
    title: stringValue(row.title, "新对话"),
    updatedAt: stringValue(row.updatedAt ?? row.updateTime ?? row.createTime, "未知"),
  }
}

function mapSettings(item: unknown): LlmSettings {
  const row = isRecord(item) ? item : {}
  return {
    vaultId: numberValue(row.vaultId, 1),
    provider: stringValue(row.provider, "OpenAI-compatible"),
    baseUrl: stringValue(row.baseUrl, ""),
    apiKeyMasked: stringValue(row.apiKeyMasked ?? row.maskedApiKey),
    model: stringValue(row.model, ""),
    maxContextSize: numberValue(row.maxContextSize, 32000),
    temperature: numberValue(row.temperature, 0.2),
    outputLanguage: stringValue(row.outputLanguage, "Chinese") as LlmSettings["outputLanguage"],
    embeddingEnabled: boolValue(row.embeddingEnabled),
    embeddingBaseUrl: stringValue(row.embeddingBaseUrl),
    embeddingApiKeyMasked: stringValue(row.embeddingApiKeyMasked),
    embeddingModel: stringValue(row.embeddingModel, "text-embedding-v4"),
    embeddingDimension: numberValue(row.embeddingDimension, 1024),
    embeddingBatchSize: numberValue(row.embeddingBatchSize, 10),
    vectorBackend: "none",
    rerankerEnabled: false,
  }
}

function mapVault(item: unknown): VaultProject {
  const row = isRecord(item) ? item : {}
  return {
    id: numberValue(row.id ?? row.vaultId, 1),
    name: stringValue(row.name ?? row.vaultName, "AI Wiki Vault"),
    path: stringValue(row.path),
    purpose: stringValue(row.purpose, "绑定本地 Obsidian Vault 后开始构建 AI Wiki。"),
    health: stringValue(row.status, "READY").toUpperCase() === "READY" ? "ready" : "needs-setup",
    lastIndexedAt: stringValue(row.lastIndexedAt ?? row.last_indexed_at, "未知"),
  }
}

function mapMetrics(item: unknown): Metric[] {
  const row = isRecord(item) ? item : {}
  const metrics = row.metrics
  if (Array.isArray(metrics)) return metrics as Metric[]

  return [
    {
      label: "原始资料",
      value: String(numberValue(row.sourceCount)),
      description: "已导入资料",
      tone: "teal",
      icon: "archive",
    },
    {
      label: "Wiki 页面",
      value: String(numberValue(row.wikiPageCount)),
      description: "可检索 Markdown",
      tone: "blue",
      icon: "file",
    },
    {
      label: "运行任务",
      value: String(numberValue(row.activeTaskCount ?? row.taskCount)),
      description: "队列处理中",
      tone: "violet",
      icon: "database",
    },
    {
      label: "失败任务",
      value: String(numberValue(row.failedTaskCount)),
      description: "需要重试或检查",
      tone: "red",
      icon: "alert",
    },
  ]
}

export const dashboardApi = {
  async overview(vaultId = 1): Promise<DashboardOverview> {
    const data = await request<unknown>("/api/dashboard/overview", {}, { vaultId })
    const row = isRecord(data) ? data : {}
    // 后端返回平铺结构，recentSources 是资料摘要列表
    const sourcePage = pageRecords(row.recentSources ?? row.sources, mapSourceDocument)
    // activeTask 是后端返回的 ActiveTaskItem，字段与 IngestTaskVO 兼容
    const activeTask = row.activeTask ? mapTask(row.activeTask) : undefined

    return {
      vaultProject: mapVault(row),   // 响应是平铺的，直接传 row
      metrics: mapMetrics(row),
      recentSources: sourcePage.records,
      activeTask,
    }
  },
}

export const sourceApi = {
  async page(vaultId = 1): Promise<SourcePage> {
    const data = await request<unknown>("/api/sources/page", {}, { vaultId, pageNo: 1, pageSize: 50 })
    const page = pageRecords(data, mapSourceDocument)
    return { records: page.records, total: page.total }
  },
  async upload(file: File, vaultId = 1): Promise<SourceDocument> {
    const form = new FormData()
    form.append("file", file)
    form.append("vaultId", String(vaultId))
    return mapSourceDocument(await request<unknown>("/api/sources/upload", { method: "POST", body: form }))
  },
  async importUrl(url: string, vaultId = 1): Promise<SourceDocument> {
    return mapSourceDocument(
      await request<unknown>("/api/sources/import-url", {
        method: "POST",
        body: JSON.stringify({ vaultId, url }),
      }),
    )
  },
  async preview(sourceId: string): Promise<SourcePreview> {
    const data = await request<unknown>("/api/sources/preview", {}, { id: sourceId })
    const row = isRecord(data) ? data : {}
    return {
      sourceId,
      title: stringValue(row.title, "解析预览"),
      extractedTextPath: stringValue(row.extractedTextPath),
      content: stringValue(row.content ?? row.text ?? row.preview),
    }
  },
  parse(sourceId: string) {
    return request<unknown>(`/api/sources/${sourceId}/parse`, { method: "POST" })
  },
  ingest(sourceId: string) {
    return request<unknown>(`/api/sources/${sourceId}/ingest`, { method: "POST" })
  },
}

export const taskApi = {
  async page(vaultId = 1): Promise<TaskPage> {
    const data = await request<unknown>("/api/tasks/page", {}, { vaultId, pageNo: 1, pageSize: 50 })
    const page = pageRecords(data, mapTask)
    return { records: page.records, total: page.total }
  },
  retry(taskId: string) {
    return request<unknown>(`/api/tasks/${taskId}/retry`, { method: "POST" })
  },
  cancel(taskId: string) {
    return request<unknown>(`/api/tasks/${taskId}/cancel`, { method: "POST" })
  },
  stream(onTask: (task: IngestTask) => void, onError: () => void, vaultId = 1) {
    const events = new EventSource(buildUrl("/api/tasks/stream", { vaultId }))
    // 后端 SSE 数据结构：{ type: "PROGRESS"|"SNAPSHOT"|"DONE"|"ERROR", task: IngestTaskVO }
    // 必须先解包 task 字段再 mapTask，否则会产生 taskId="" 的幽灵任务
    const handle = (event: MessageEvent) => {
      const parsed = JSON.parse(event.data as string) as unknown
      const taskData = isRecord(parsed) && parsed.task ? parsed.task : parsed
      onTask(mapTask(taskData))
    }
    // 监听后端所有命名事件类型（onmessage 只处理无类型的 message 事件，不适用）
    for (const name of ["progress", "snapshot", "done", "error"]) {
      events.addEventListener(name, handle)
    }
    events.onerror = () => onError()
    return () => events.close()
  },
  clear(vaultId = 1) {
    return request<unknown>("/api/tasks/clear", { method: "DELETE" }, { vaultId })
  },
}

export const wikiApi = {
  async tree(vaultId = 1): Promise<WikiTreeNode[]> {
    const data = await request<unknown>("/api/wiki/tree", {}, { vaultId })
    return Array.isArray(data) ? data.map(mapTreeNode) : []
  },
  async page(path: string, vaultId = 1): Promise<WikiPage> {
    return mapWikiPage(await request<unknown>("/api/wiki/page", {}, { vaultId, path }))
  },
  async search(keyword: string, vaultId = 1): Promise<WikiPage[]> {
    const data = await request<unknown>("/api/wiki/search", {}, { vaultId, query: keyword })
    return pageRecords(data, mapWikiPage).records
  },
  open(path: string, vaultId = 1) {
    return request<unknown>("/api/wiki/open", {
      method: "POST",
      body: JSON.stringify({ vaultId, path }),
    })
  },
}

export const chatApi = {
  async sessions(vaultId = 1): Promise<ChatSession[]> {
    const data = await request<unknown>("/api/chat/sessions", {}, { vaultId })
    return pageRecords(data, mapSession).records
  },
  async createSession(vaultId = 1): Promise<ChatSession> {
    return mapSession(await request<unknown>("/api/chat/session", {
      method: "POST",
      body: JSON.stringify({ vaultId, title: "新对话" }),
    }))
  },
  async messages(sessionId: string): Promise<ChatMessage[]> {
    const data = await request<unknown>("/api/chat/messages", {}, { sessionId })
    return pageRecords(data, mapChatMessage).records
  },
  async saveAnswer(payload: {
    vaultId?: number
    sessionId: string
    messageId: string
    targetType: "synthesis" | "question"
  }) {
    const folder = payload.targetType === "synthesis" ? "wiki/synthesis" : "wiki/questions"
    const safeId = payload.messageId.replace(/[^a-zA-Z0-9_-]/g, "-")
    const targetPath = `${folder}/${new Date().toISOString().slice(0, 10)}-${safeId}.md`

    return request<unknown>("/api/chat/save-answer", {
      method: "POST",
      body: JSON.stringify({ messageId: Number(payload.messageId), targetPath }),
    }, { vaultId: payload.vaultId ?? 1 })
  },
  async stream(
    payload: { vaultId?: number; sessionId: string; question: string; maxReferences: number },
    onEvent: (event: { event: string; data: unknown }) => void,
  ) {
    const body = {
      vaultId: payload.vaultId ?? 1,
      sessionId: Number(payload.sessionId),
      question: payload.question,
      maxReferences: payload.maxReferences,
    }
    const response = await fetch(buildUrl("/api/chat/stream"), {
      method: "POST",
      headers: { Accept: "text/event-stream", "Content-Type": "application/json" },
      body: JSON.stringify(body),
    })

    if (!response.ok || !response.body) throw new ApiError(response.statusText, response.status)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ""

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const chunks = buffer.split("\n\n")
      buffer = chunks.pop() ?? ""
      chunks.forEach((chunk) => {
        const lines = chunk.split("\n")
        const event = lines.find((line) => line.startsWith("event:"))?.slice(6).trim() ?? "message"
        const data = lines
          .filter((line) => line.startsWith("data:"))
          .map((line) => line.slice(5).trim())
          .join("\n")
        if (!data) return
        const parsed = JSON.parse(data) as unknown
        const mapped =
          event === "reference" && Array.isArray(parsed)
            ? parsed.map((item, idx) => mapReference({ ...(isRecord(item) ? item : {}), id: idx + 1 }))
            : parsed
        onEvent({ event, data: mapped })
      })
    }
  },
}

export const settingsApi = {
  async detail(vaultId = 1): Promise<LlmSettings> {
    return mapSettings(await request<unknown>("/api/settings/detail", {}, { vaultId }))
  },
  async update(settings: LlmSettings): Promise<LlmSettings> {
    return mapSettings(await request<unknown>("/api/settings/update", {
      method: "PUT",
      body: JSON.stringify(settings),
    }))
  },
  testLlm(settings: LlmSettings) {
    return request<unknown>("/api/settings/test-llm", {
      method: "POST",
      body: JSON.stringify(settings),
    })
  },
}

export const embeddingApi = {
  async stats(vaultId = 1): Promise<EmbeddingStats> {
    const data = await request<unknown>("/api/embedding/stats", {}, { vaultId })
    const row = isRecord(data) ? data : {}
    return {
      total: numberValue(row.total),
      success: numberValue(row.success),
      failed: numberValue(row.failed),
      pending: numberValue(row.pending),
      lastEmbeddedAt: stringValue(row.lastEmbeddedAt),
      failedPages: Array.isArray(row.failedPages)
        ? row.failedPages.map((p: unknown) => {
            const r = isRecord(p) ? p : {}
            return {
              pageId: numberValue(r.pageId),
              path: stringValue(r.path),
              title: stringValue(r.title),
              error: stringValue(r.error),
            }
          })
        : [],
    }
  },

  async test(config: { baseUrl: string; apiKey: string; model: string; dimension?: number }) {
    return request<unknown>("/api/embedding/test", {
      method: "POST",
      body: JSON.stringify(config),
    })
  },

  async pages(vaultId = 1): Promise<EmbeddingPageStatus[]> {
    const data = await request<unknown>("/api/embedding/pages", {}, { vaultId })
    return Array.isArray(data)
      ? data.map((item: unknown) => {
          const row = isRecord(item) ? item : {}
          return {
            pageId: numberValue(row.pageId),
            path: stringValue(row.path),
            title: stringValue(row.title),
            type: stringValue(row.type),
            embedStatus: stringValue(row.embedStatus, "PENDING"),
            embeddingModel: stringValue(row.embeddingModel),
            embeddedAt: stringValue(row.embeddedAt),
            error: stringValue(row.error),
          }
        })
      : []
  },

  async rebuild(vaultId: number, mode: "pending" | "failed" | "all") {
    return request<unknown>("/api/embedding/rebuild", {
      method: "POST",
      body: JSON.stringify({ vaultId, mode }),
    })
  },

  async progress(vaultId = 1): Promise<EmbeddingProgress> {
    const data = await request<unknown>("/api/embedding/progress", {}, { vaultId })
    const row = isRecord(data) ? data : {}
    return {
      processing: boolValue(row.processing),
      current: numberValue(row.current),
      total: numberValue(row.total),
    }
  },

  async embedPage(pageId: number) {
    return request<unknown>(`/api/embedding/page/${pageId}`, { method: "POST" })
  },
}
