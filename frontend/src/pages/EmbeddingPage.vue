<script setup lang="ts">
import { inject, onMounted, onUnmounted, ref } from "vue"
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NEmpty,
  NProgress,
  NSpace,
  NSpin,
  NTag,
  type DataTableColumns,
} from "naive-ui"
import { h } from "vue"
import AppIcon from "../components/AppIcon.vue"
import { embeddingApi, settingsApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import type { EmbeddingPageStatus, EmbeddingStats, PageKey } from "../types"

const navigateTo = inject<(page: PageKey) => void>("navigateTo")

const loading = ref(true)
const embeddingEnabled = ref(false)
const actionLoading = ref<string | null>(null)
const errorMessage = ref("")
const successMessage = ref("")
const stats = ref<EmbeddingStats>({
  total: 0, success: 0, failed: 0, pending: 0,
  lastEmbeddedAt: "", failedPages: [],
})
const pages = ref<EmbeddingPageStatus[]>([])
const processing = ref(false)
const progressCurrent = ref(0)
const progressTotal = ref(0)

let pollTimer: ReturnType<typeof setInterval> | null = null

function embedStatusType(status: string) {
  if (status === "SUCCESS") return "success"
  if (status === "FAILED") return "error"
  return "warning"
}

function embedStatusLabel(status: string) {
  if (status === "SUCCESS") return "已向量化"
  if (status === "FAILED") return "失败"
  return "待处理"
}

const pageColumns: DataTableColumns<EmbeddingPageStatus> = [
  {
    title: "Wiki 页面",
    key: "title",
    render(row) {
      return h("div", { class: "embed-page-title" }, [
        h("strong", row.title || row.path),
        h("span", row.path),
      ])
    },
  },
  {
    title: "状态",
    key: "embedStatus",
    width: 120,
    render(row) {
      return h(NTag, { type: embedStatusType(row.embedStatus), size: "small", bordered: false }, {
        default: () => embedStatusLabel(row.embedStatus),
      })
    },
  },
  {
    title: "模型",
    key: "embeddingModel",
    width: 190,
    render(row) {
      return row.embeddingModel || "-"
    },
  },
  {
    title: "最近向量化",
    key: "embeddedAt",
    width: 170,
    render(row) {
      return row.embeddedAt ? row.embeddedAt.slice(0, 16).replace("T", " ") : "-"
    },
  },
  {
    title: "操作",
    key: "actions",
    width: 150,
    render(row) {
      return h(NButton, {
        size: "small",
        secondary: true,
        type: row.embedStatus === "SUCCESS" ? "default" : "primary",
        loading: actionLoading.value === `page-${row.pageId}`,
        disabled: !embeddingEnabled.value || processing.value,
        onClick: () => retryPage(row.pageId),
      }, {
        icon: () => h(AppIcon, { name: row.embedStatus === "SUCCESS" ? "refresh" : "play" }),
        default: () => row.embedStatus === "SUCCESS" ? "重新向量化" : "向量化",
      })
    },
  },
]

async function loadSettings() {
  try {
    const settings = await settingsApi.detail()
    embeddingEnabled.value = settings.embeddingEnabled
  } catch (e) {
    errorMessage.value = toErrorMessage(e)
  }
}

async function loadStats() {
  try {
    const [nextStats, nextPages] = await Promise.all([
      embeddingApi.stats(1),
      embeddingApi.pages(1),
    ])
    stats.value = nextStats
    pages.value = nextPages
  } catch (e) {
    errorMessage.value = toErrorMessage(e)
  } finally {
    loading.value = false
  }
}

async function pollProgress() {
  try {
    const p = await embeddingApi.progress(1)
    processing.value = p.processing
    progressCurrent.value = p.current
    progressTotal.value = p.total
    if (!p.processing) {
      await loadStats()
    }
  } catch { /* ignore poll errors */ }
}

async function triggerRebuild(mode: "pending" | "failed" | "all") {
  actionLoading.value = mode
  errorMessage.value = ""
  successMessage.value = ""
  try {
    const result = await embeddingApi.rebuild(1, mode) as Record<string, unknown>
    if (result?.started) {
      successMessage.value = String(result.message ?? "批量向量化已启动")
      processing.value = true
    } else {
      errorMessage.value = String(result?.message ?? "启动失败")
    }
  } catch (e) {
    errorMessage.value = toErrorMessage(e)
  } finally {
    actionLoading.value = null
  }
}

async function retryPage(pageId: number) {
  actionLoading.value = `page-${pageId}`
  try {
    await embeddingApi.embedPage(pageId)
    await loadStats()
    successMessage.value = "页面向量化完成"
  } catch (e) {
    errorMessage.value = toErrorMessage(e)
  } finally {
    actionLoading.value = null
  }
}

const progressPercent = () =>
  progressTotal.value > 0 ? Math.round((progressCurrent.value / progressTotal.value) * 100) : 0

const successPercent = () =>
  stats.value.total > 0 ? Math.round((stats.value.success / stats.value.total) * 100) : 0

onMounted(async () => {
  await Promise.all([loadSettings(), loadStats()])
  pollTimer = setInterval(pollProgress, 2000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <div class="embedding-page">
    <NSpin :show="loading">
      <NSpace vertical :size="16">

        <!-- 错误 / 成功提示 -->
        <NAlert v-if="errorMessage" type="error" :title="errorMessage" closable @close="errorMessage = ''" />
        <NAlert v-if="successMessage" type="success" :title="successMessage" closable @close="successMessage = ''" />
        <NAlert v-if="!loading && !embeddingEnabled" type="warning" :bordered="false">
          向量搜索尚未启用。请先到设置页打开 embedding 并保存向量化模型配置，之后才能执行批量或单页向量化。
          <NButton text type="primary" @click="navigateTo?.('settings')">前往设置</NButton>
        </NAlert>

        <!-- 状态统计卡 -->
        <NCard title="状态统计">
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ stats.total }}</span>
              <span class="stat-label">总页面</span>
            </div>
            <div class="stat-item stat-item--success">
              <span class="stat-value">{{ stats.success }}</span>
              <span class="stat-label">已向量化</span>
            </div>
            <div class="stat-item stat-item--pending">
              <span class="stat-value">{{ stats.pending }}</span>
              <span class="stat-label">待处理</span>
            </div>
            <div class="stat-item stat-item--failed">
              <span class="stat-value">{{ stats.failed }}</span>
              <span class="stat-label">失败</span>
            </div>
          </div>

          <div class="progress-row">
            <NProgress
              type="line"
              :percentage="processing ? progressPercent() : successPercent()"
              :status="processing ? 'info' : 'success'"
              :show-indicator="false"
            />
            <span class="progress-label">
              <template v-if="processing">
                处理中 {{ progressCurrent }}/{{ progressTotal }}
              </template>
              <template v-else>
                {{ successPercent() }}% 已完成
                <template v-if="stats.lastEmbeddedAt">
                  · 最近向量化: {{ stats.lastEmbeddedAt.slice(0, 16).replace('T', ' ') }}
                </template>
              </template>
            </span>
          </div>
        </NCard>

        <!-- 操作卡 -->
        <NCard title="操作">
          <NSpace>
            <NButton
              type="primary"
              :loading="actionLoading === 'pending'"
              :disabled="!embeddingEnabled || processing || stats.pending === 0"
              @click="triggerRebuild('pending')"
            >
              <template #icon><AppIcon name="play" /></template>
              向量化未处理页面 ({{ stats.pending }})
            </NButton>
            <NButton
              type="warning"
              :loading="actionLoading === 'failed'"
              :disabled="!embeddingEnabled || processing || stats.failed === 0"
              @click="triggerRebuild('failed')"
            >
              <template #icon><AppIcon name="refresh" /></template>
              重试失败页面 ({{ stats.failed }})
            </NButton>
            <NButton
              type="error"
              secondary
              :loading="actionLoading === 'all'"
              :disabled="!embeddingEnabled || processing || stats.total === 0"
              @click="triggerRebuild('all')"
            >
              <template #icon><AppIcon name="refresh" /></template>
              全部重新向量化 ({{ stats.total }})
            </NButton>
          </NSpace>
          <p class="tip">⚠ 全部重新向量化会消耗 API 配额，请谨慎操作。</p>
        </NCard>

        <NCard title="单个文件向量化">
          <template #header-extra>
            <NButton tertiary :loading="loading" @click="loadStats">
              <template #icon><AppIcon name="refresh" /></template>
              刷新
            </NButton>
          </template>
          <NDataTable
            :columns="pageColumns"
            :data="pages"
            :bordered="false"
            :pagination="{ pageSize: 8 }"
          />
        </NCard>

        <!-- 失败列表 -->
        <NCard v-if="stats.failedPages.length > 0" title="失败页面">
          <div
            v-for="page in stats.failedPages"
            :key="page.pageId"
            class="failed-item"
          >
            <div class="failed-item__info">
              <NTag type="error" size="small">失败</NTag>
              <span class="failed-item__path">{{ page.path }}</span>
            </div>
            <p class="failed-item__error">{{ page.error }}</p>
            <NButton
              size="small"
              :loading="actionLoading === `page-${page.pageId}`"
              @click="retryPage(page.pageId)"
            >
              重试
            </NButton>
          </div>
        </NCard>

        <NEmpty
          v-else-if="!loading && stats.total === 0"
          description="暂无 Wiki 页面，请先完成资料摄入"
        >
          <template #extra>
            <NButton @click="navigateTo?.('sources')">前往资料导入</NButton>
          </template>
        </NEmpty>

      </NSpace>
    </NSpin>
  </div>
</template>

<style scoped>
.embedding-page { padding: 0; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  background: rgba(255,255,255,0.04);
}
.stat-item--success { background: rgba(16,185,129,0.1); }
.stat-item--pending { background: rgba(245,158,11,0.1); }
.stat-item--failed  { background: rgba(239,68,68,0.1); }

.stat-value { font-size: 28px; font-weight: 700; line-height: 1; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 4px; }

.progress-row { display: flex; align-items: center; gap: 12px; margin-top: 8px; }
.progress-label { font-size: 12px; color: #94a3b8; white-space: nowrap; }

.tip { font-size: 12px; color: #94a3b8; margin-top: 12px; }

.embed-page-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.embed-page-title strong {
  font-size: 13px;
  color: #0f172a;
}

.embed-page-title span {
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  overflow-wrap: anywhere;
}

.failed-item {
  padding: 12px;
  border-radius: 8px;
  background: rgba(239,68,68,0.06);
  margin-bottom: 8px;
}
.failed-item__info { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.failed-item__path { font-size: 13px; font-family: monospace; }
.failed-item__error { font-size: 12px; color: #f87171; margin: 4px 0 8px; }
</style>
