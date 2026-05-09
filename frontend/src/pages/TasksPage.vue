<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue"
import { NAlert, NButton, NEmpty, NProgress, NSpace, NSpin, NTimeline, NTimelineItem } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import StatusTag from "../components/StatusTag.vue"
import { embeddingApi, settingsApi, taskApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import { taskStatusLabel } from "../utils/status"
import type { IngestTask, PageKey } from "../types"

const navigateTo = inject<(page: PageKey) => void>("navigateTo")

const loading = ref(true)
const actionLoading = ref("")
const clearLoading = ref(false)
const errorMessage = ref("")
const tasks = ref<IngestTask[]>([])
const pendingEmbedCount = ref(0)
const failedEmbedCount = ref(0)
const embeddingEnabled = ref(false)
const failedTasks = computed(() => tasks.value.filter((task) => task.status === "Failed").length)
const terminatedTasks = computed(() =>
  tasks.value.filter((t) => t.status === "Cancelled" || t.status === "Failed" || t.status === "ManualCheck").length
)
let closeTaskStream: (() => void) | undefined

async function loadTasks() {
  loading.value = true
  errorMessage.value = ""
  try {
    const page = await taskApi.page()
    tasks.value = page.records
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

function mergeTask(nextTask: IngestTask) {
  const index = tasks.value.findIndex((task) => task.taskId === nextTask.taskId)
  if (index >= 0) tasks.value[index] = nextTask
  else tasks.value.unshift(nextTask)
}

async function retryTask(taskId: string) {
  actionLoading.value = taskId
  errorMessage.value = ""
  try {
    await taskApi.retry(taskId)
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    actionLoading.value = ""
  }
}

async function cancelTask(taskId: string) {
  actionLoading.value = taskId
  errorMessage.value = ""
  try {
    await taskApi.cancel(taskId)
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    actionLoading.value = ""
  }
}

async function clearTerminated() {
  clearLoading.value = true
  errorMessage.value = ""
  try {
    await taskApi.clear()
    await loadTasks()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    clearLoading.value = false
  }
}

onMounted(() => {
  void loadTasks()
  settingsApi.detail().then((settings) => {
    embeddingEnabled.value = settings.embeddingEnabled
  }).catch(() => {})
  embeddingApi.stats(1).then((s: any) => {
    pendingEmbedCount.value = s.pending ?? 0
    failedEmbedCount.value = s.failed ?? 0
  }).catch(() => {})
  closeTaskStream = taskApi.stream(mergeTask, () => {
    if (!errorMessage.value) errorMessage.value = "任务进度流暂不可用"
  })
})

onBeforeUnmount(() => {
  closeTaskStream?.()
})
</script>

<template>
  <section class="page-grid tasks-grid">
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>
    <NAlert
      v-if="!embeddingEnabled"
      type="info"
      :bordered="false"
      style="cursor:pointer"
      @click="navigateTo?.('settings')"
    >
      向量搜索尚未启用。完成资料摄入后，请先在设置页配置向量化模型。
    </NAlert>
    <NAlert
      v-else-if="pendingEmbedCount > 0 || failedEmbedCount > 0"
      type="info"
      :bordered="false"
      style="cursor:pointer"
      @click="navigateTo?.('embedding')"
    >
      共 {{ pendingEmbedCount }} 个 Wiki 页面未向量化，{{ failedEmbedCount }} 个失败，点击前往向量管理处理。
    </NAlert>

    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>摄入队列</h2>
          <p>任务默认按 Vault 串行执行，避免多个任务同时写入。</p>
        </div>
        <NButton
          secondary
          type="error"
          :disabled="terminatedTasks === 0"
          :loading="clearLoading"
          @click="clearTerminated"
        >
          <template #icon>
            <AppIcon name="trash" />
          </template>
          清除历史 ({{ terminatedTasks }})
        </NButton>
      </div>

      <NSpin v-if="loading" />
      <NEmpty v-else-if="tasks.length === 0" description="暂无摄入任务" />
      <div v-else class="task-list">
        <div v-for="task in tasks" :key="task.taskId" class="task-row">
          <div class="task-row__header">
            <div>
              <strong>{{ task.sourceTitle }}</strong>
              <span>{{ task.taskId }} · {{ task.updatedAt }}</span>
            </div>
            <StatusTag :status="task.status" />
          </div>
          <NProgress
            type="line"
            :percentage="task.progress"
            :status="task.status === 'Failed' ? 'error' : 'default'"
            :height="8"
            :border-radius="4"
            :fill-border-radius="4"
          />
          <div class="task-row__meta">
            <span>状态：{{ taskStatusLabel(task.status) }}</span>
            <span>重试：{{ task.retryCount }}/3</span>
            <span>写入：{{ task.writtenFiles.length ? task.writtenFiles.join("、") : "暂无" }}</span>
          </div>
          <p v-if="task.errorMessage" class="error-text">{{ task.errorMessage }}</p>
          <NSpace :size="8" class="task-actions">
            <NButton
              size="small"
              tertiary
              :loading="actionLoading === task.taskId"
              :disabled="task.status === 'Done' || task.status === 'Cancelled'"
              @click="cancelTask(task.taskId)"
            >
              <template #icon>
                <AppIcon name="x" />
              </template>
              取消
            </NButton>
            <NButton
              size="small"
              secondary
              :loading="actionLoading === task.taskId"
              :disabled="!['Failed', 'ManualCheck', 'Cancelled'].includes(task.status)"
              @click="retryTask(task.taskId)"
            >
              <template #icon>
                <AppIcon name="refresh" />
              </template>
              重试
            </NButton>
          </NSpace>
        </div>
      </div>
    </div>

    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>执行日志</h2>
          <p>展示两阶段 AI 摄入、FILE block 校验和写入结果。</p>
        </div>
      </div>
      <NTimeline>
        <NTimelineItem
          type="success"
          title="读取 source 文本"
          content="PDF 文本和图片引用已解析"
        />
        <NTimelineItem
          type="info"
          title="第一阶段：资料分析"
          content="生成摘要、实体、概念、矛盾点和页面建议"
        />
        <NTimelineItem
          type="warning"
          title="第二阶段：FILE block 校验"
          content="校验路径、frontmatter 和 END FILE 标记"
        />
        <NTimelineItem
          type="success"
          title="写入 Wiki"
          content="更新 wiki/sources、wiki/index.md 和 wiki/log.md"
        />
        <NTimelineItem
          title="索引同步"
          content="关键词索引刷新，向量库 upsert（如启用）"
        />
      </NTimeline>
    </div>
  </section>
</template>
