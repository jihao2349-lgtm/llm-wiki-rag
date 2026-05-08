<script setup lang="ts">
import { inject, onMounted, onUnmounted, ref } from "vue"
import {
  NAlert,
  NButton,
  NCard,
  NEmpty,
  NProgress,
  NSpace,
  NSpin,
  NTag,
} from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { embeddingApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import type { EmbeddingStats, PageKey } from "../types"

const navigateTo = inject<(page: PageKey) => void>("navigateTo")

const loading = ref(true)
const actionLoading = ref<string | null>(null)
const errorMessage = ref("")
const successMessage = ref("")
const stats = ref<EmbeddingStats>({
  total: 0, success: 0, failed: 0, pending: 0,
  lastEmbeddedAt: "", failedPages: [],
})
const processing = ref(false)
const progressCurrent = ref(0)
const progressTotal = ref(0)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function loadStats() {
  try {
    stats.value = await embeddingApi.stats(1)
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
    successMessage.value = "重试成功"
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
  await loadStats()
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
              :disabled="processing || stats.pending === 0"
              @click="triggerRebuild('pending')"
            >
              <template #icon><AppIcon name="play" /></template>
              向量化未处理页面 ({{ stats.pending }})
            </NButton>
            <NButton
              type="warning"
              :loading="actionLoading === 'failed'"
              :disabled="processing || stats.failed === 0"
              @click="triggerRebuild('failed')"
            >
              <template #icon><AppIcon name="refresh" /></template>
              重试失败页面 ({{ stats.failed }})
            </NButton>
            <NButton
              type="error"
              secondary
              :loading="actionLoading === 'all'"
              :disabled="processing || stats.total === 0"
              @click="triggerRebuild('all')"
            >
              <template #icon><AppIcon name="refresh" /></template>
              全部重新向量化 ({{ stats.total }})
            </NButton>
          </NSpace>
          <p class="tip">⚠ 全部重新向量化会消耗 API 配额，请谨慎操作。</p>
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
