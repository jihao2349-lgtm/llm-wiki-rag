<script setup lang="ts">
import { ref } from "vue"
import { NButton, NDataTable, NInput, NSelect, NSpace, NTag, type DataTableColumns } from "naive-ui"
import { h } from "vue"
import AppIcon from "../components/AppIcon.vue"
import { apiCallLogs, ragSampleChunks } from "../mock-data"
import type { ApiCallLog } from "../types"

const queryInput = ref("Agent Memory 设计模式")
const mode = ref<"retrieval" | "answer">("retrieval")
const topK = ref(5)

const modeOptions = [
  { label: "Retrieval (返回片段)", value: "retrieval" },
  { label: "Answer (LLM 生成)", value: "answer" },
]

const sampleResponse = `{
  "code": 200,
  "data": {
    "chunks": [
      {
        "path": "wiki/concepts/agent-memory.md",
        "title": "Agent Memory",
        "score": 0.87,
        "content": "Agent Memory 通常分为三层..."
      }
    ],
    "totalTokens": 3580,
    "queryId": "q-20260506-001"
  }
}`

const logColumns: DataTableColumns<ApiCallLog> = [
  {
    title: "Endpoint",
    key: "endpoint",
    width: 220,
    render(row) {
      return h("span", { style: "font-family: 'JetBrains Mono', monospace; font-size: 0.84rem" }, row.endpoint)
    },
  },
  { title: "Key", key: "apiKeyName", width: 140 },
  {
    title: "查询",
    key: "query",
    render(row) {
      return h("span", { style: "color: var(--color-text-muted)" }, row.query)
    },
  },
  {
    title: "延迟",
    key: "latencyMs",
    width: 80,
    render(row) {
      return `${row.latencyMs}ms`
    },
  },
  {
    title: "状态",
    key: "status",
    width: 100,
    render(row) {
      const map = {
        success: { type: "success", label: "成功" },
        error: { type: "error", label: "失败" },
        rate_limited: { type: "warning", label: "限流" },
      } as const
      const { type, label } = map[row.status]
      return h(NTag, { type, round: true, bordered: false, size: "small" }, () => label)
    },
  },
  { title: "时间", key: "createdAt", width: 100 },
]
</script>

<template>
  <section class="page-grid">
    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>RAG Playground</h2>
          <p>在线测试外部 API。生产环境通过 API Key 鉴权，调用 /api/v1/rag/*。</p>
        </div>
        <NButton secondary>
          <template #icon>
            <AppIcon name="external" />
          </template>
          导出 OpenAPI
        </NButton>
      </div>

      <div class="page-grid console-grid">
        <div class="surface-card surface-card--soft">
          <div class="section-toolbar">
            <div>
              <h2 style="font-size: 0.96rem">请求</h2>
              <p>POST /api/v1/rag/{{ mode === 'retrieval' ? 'search' : 'answer' }}</p>
            </div>
          </div>
          <NSpace vertical :size="12">
            <div class="form-grid">
              <NSelect v-model:value="mode" :options="modeOptions" />
              <NInput :value="`topK = ${topK}`" :readonly="true" />
            </div>
            <NInput
              v-model:value="queryInput"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 6 }"
              placeholder="输入查询语句"
            />
            <NSpace :size="8">
              <NButton type="primary">
                <template #icon>
                  <AppIcon name="play" />
                </template>
                运行
              </NButton>
              <NButton tertiary>
                <template #icon>
                  <AppIcon name="copy" />
                </template>
                复制 cURL
              </NButton>
            </NSpace>
          </NSpace>
        </div>

        <div class="surface-card surface-card--soft">
          <div class="section-toolbar">
            <div>
              <h2 style="font-size: 0.96rem">响应</h2>
              <p>HTTP 200 · 142ms · {{ ragSampleChunks.length }} chunks</p>
            </div>
          </div>
          <pre class="code-block">{{ sampleResponse }}</pre>
        </div>
      </div>

      <div style="margin-top: 18px">
        <div class="section-toolbar">
          <div>
            <h2 style="font-size: 0.96rem">召回片段预览</h2>
            <p>按相似度排序，分数越高越相关。</p>
          </div>
        </div>
        <NSpace vertical :size="10">
          <div v-for="chunk in ragSampleChunks" :key="chunk.path" class="rag-chunk">
            <div class="rag-chunk__head">
              <div>
                <span class="rag-chunk__title">{{ chunk.title }}</span>
                <span class="rag-chunk__path"> · {{ chunk.path }}</span>
              </div>
              <span class="score-pill">score {{ chunk.score.toFixed(2) }}</span>
            </div>
            <div class="rag-chunk__body">{{ chunk.content }}</div>
          </div>
        </NSpace>
      </div>
    </div>

    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>调用日志</h2>
          <p>外部 API 最近的调用记录，用于审计和性能监控。</p>
        </div>
        <NButton secondary>
          <template #icon>
            <AppIcon name="refresh" />
          </template>
          刷新
        </NButton>
      </div>
      <NDataTable :columns="logColumns" :data="apiCallLogs" :bordered="false" />
    </div>
  </section>
</template>
