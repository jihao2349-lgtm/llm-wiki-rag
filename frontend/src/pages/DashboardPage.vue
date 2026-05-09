<script setup lang="ts">
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
import { h, inject, onMounted, ref } from "vue"
import AppIcon from "../components/AppIcon.vue"
import MetricCard from "../components/MetricCard.vue"
import StatusTag from "../components/StatusTag.vue"
import { dashboardApi, embeddingApi, settingsApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, IngestTask, Metric, PageKey, SourceDocument, VaultProject } from "../types"

const navigateTo = inject<(page: PageKey) => void>('navigateTo')

const loading = ref(true)
const errorMessage = ref("")
const metrics = ref<Metric[]>([])
const sources = ref<SourceDocument[]>([])
const activeTask = ref<IngestTask>()
const pendingEmbedCount = ref(0)
const failedEmbedCount = ref(0)
const embeddingEnabled = ref(false)
const vaultProject = ref<VaultProject>({
  name: "AI Wiki Vault",
  path: "",
  purpose: "绑定本地 Obsidian Vault 后开始构建 AI Wiki。",
  health: "needs-setup",
  lastIndexedAt: "未知",
})

const sourceColumns: DataTableColumns<SourceDocument> = [
  {
    title: "资料",
    key: "title",
    render(row) {
      return h("div", { class: "table-title" }, [
        h("strong", row.title),
        h("span", `${row.updatedAt} · ${row.size ?? ""}`),
      ])
    },
  },
  {
    title: "模态",
    key: "modality",
    width: 110,
    render(row) {
      return h(
        "span",
        { class: `modality-chip modality-chip--${row.modality}` },
        [
          h(AppIcon, { name: modalityIcon(row.modality) as IconName, size: 12 }),
          modalityLabel(row.modality),
        ],
      )
    },
  },
  {
    title: "状态",
    key: "status",
    width: 110,
    render(row) {
      return h(StatusTag, { status: row.status })
    },
  },
  { title: "目标页面", key: "targetPage" },
]

async function loadOverview() {
  loading.value = true
  errorMessage.value = ""
  try {
    const [overview, embedStats, settings] = await Promise.all([
      dashboardApi.overview(),
      embeddingApi.stats(1).catch(() => null),
      settingsApi.detail().catch(() => null),
    ])
    vaultProject.value = overview.vaultProject
    metrics.value = overview.metrics
    sources.value = overview.recentSources
    activeTask.value = overview.activeTask
    embeddingEnabled.value = Boolean(settings?.embeddingEnabled)
    if (embedStats) {
      pendingEmbedCount.value = (embedStats as any).pending ?? 0
      failedEmbedCount.value = (embedStats as any).failed ?? 0
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<template>
  <section class="page-grid dashboard-grid">
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>

    <div class="workspace-header">
      <div>
        <NTag :type="vaultProject.health === 'ready' ? 'success' : 'warning'" round :bordered="false">
          <template #icon>
            <AppIcon :name="vaultProject.health === 'ready' ? 'check' : 'alert'" />
          </template>
          {{ vaultProject.health === "ready" ? "Vault ready" : "Needs setup" }}
        </NTag>
        <h2>{{ vaultProject.name }}</h2>
        <p>{{ vaultProject.purpose }}</p>
      </div>
      <div class="path-panel">
        <span>Vault 路径</span>
        <strong>{{ vaultProject.path }}</strong>
        <small>索引同步：{{ vaultProject.lastIndexedAt }}</small>
      </div>
    </div>

    <div v-if="loading" class="section-panel">
      <NSpin />
    </div>

    <div v-else class="metric-grid">
      <MetricCard v-for="item in metrics" :key="item.label" :metric="item" />
    </div>

    <NAlert
      v-if="!embeddingEnabled"
      type="info"
      :bordered="false"
      style="cursor:pointer"
      @click="navigateTo?.('settings')"
    >
      向量搜索尚未启用。前往设置配置向量化模型后，可在“向量管理”执行单页或批量向量化。
    </NAlert>

    <NAlert
      v-else-if="pendingEmbedCount > 0 || failedEmbedCount > 0"
      type="warning"
      :bordered="false"
      style="cursor:pointer"
      @click="navigateTo?.('embedding')"
    >
      {{ pendingEmbedCount }} 个 Wiki 页面待向量化，{{ failedEmbedCount }} 个失败，点击前往向量管理处理。
    </NAlert>

    <div class="two-column">
      <NCard title="知识生产链路">
        <template #header-extra>
          <NButton secondary @click="navigateTo?.('tasks')">
            <template #icon>
              <AppIcon name="play" />
            </template>
            运行摄入
          </NButton>
        </template>
        <div class="pipeline">
          <div class="pipeline-step done">
            <div class="pipeline-step__icon">
              <AppIcon name="upload" />
            </div>
            <strong>资料导入</strong>
            <span>文件、URL 统一进入 raw/</span>
          </div>
          <div class="pipeline-step active">
            <div class="pipeline-step__icon">
              <AppIcon name="spark" />
            </div>
            <strong>AI 两阶段摄入</strong>
            <span>分析资料后生成 FILE block</span>
          </div>
          <div class="pipeline-step">
            <div class="pipeline-step__icon">
              <AppIcon name="file" />
            </div>
            <strong>写入 Wiki</strong>
            <span>路径校验，仅允许 wiki/</span>
          </div>
          <div class="pipeline-step">
            <div class="pipeline-step__icon">
              <AppIcon name="search" />
            </div>
            <strong>RAG 检索</strong>
            <span>关键词 + wikilink</span>
          </div>
        </div>
      </NCard>

      <NCard title="当前任务">
        <div v-if="activeTask" class="task-summary">
          <div>
            <strong>{{ activeTask.sourceTitle }}</strong>
            <span>{{ activeTask.taskId }}</span>
          </div>
          <StatusTag :status="activeTask.status" />
          <NProgress
            type="line"
            :percentage="activeTask.progress"
            :height="8"
            :border-radius="4"
            :fill-border-radius="4"
          />
          <small>已写入：{{ activeTask.writtenFiles.join("、") || "暂无" }}</small>
        </div>
        <NEmpty v-else description="暂无运行中的摄入任务" />
      </NCard>
    </div>

    <NCard title="最近资料">
      <template #header-extra>
        <NSpace :size="8">
          <NButton secondary :loading="loading" @click="loadOverview">
            <template #icon>
              <AppIcon name="refresh" />
            </template>
            重建索引
          </NButton>
          <NButton type="primary" @click="navigateTo?.('chat')">
            <template #icon>
              <AppIcon name="message" />
            </template>
            开始对话
          </NButton>
        </NSpace>
      </template>
      <NDataTable :columns="sourceColumns" :data="sources.slice(0, 4)" :loading="loading" :bordered="false" />
    </NCard>
  </section>
</template>
