<script setup lang="ts">
import { NButton, NCard, NDataTable, NProgress, NSpace, NTag, type DataTableColumns } from "naive-ui"
import { h } from "vue"
import AppIcon from "../components/AppIcon.vue"
import MetricCard from "../components/MetricCard.vue"
import StatusTag from "../components/StatusTag.vue"
import { metrics, sources, tasks, vaultProject } from "../mock-data"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, SourceDocument } from "../types"

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

const activeTask = tasks.find((task) => task.status === "Processing")
</script>

<template>
  <section class="page-grid dashboard-grid">
    <div class="workspace-header">
      <div>
        <NTag type="success" round :bordered="false">
          <template #icon>
            <AppIcon name="check" />
          </template>
          Vault ready
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

    <div class="metric-grid">
      <MetricCard v-for="item in metrics" :key="item.label" :metric="item" />
    </div>

    <div class="two-column">
      <NCard title="知识生产链路">
        <template #header-extra>
          <NButton secondary>
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
            <strong>多模态投喂</strong>
            <span>文件、URL、音视频统一进入 raw/</span>
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
            <span>关键词 + 向量 + wikilink</span>
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
      </NCard>
    </div>

    <NCard title="最近资料">
      <template #header-extra>
        <NSpace :size="8">
          <NButton secondary>
            <template #icon>
              <AppIcon name="refresh" />
            </template>
            重建索引
          </NButton>
          <NButton type="primary">
            <template #icon>
              <AppIcon name="message" />
            </template>
            开始对话
          </NButton>
        </NSpace>
      </template>
      <NDataTable :columns="sourceColumns" :data="sources.slice(0, 4)" :bordered="false" />
    </NCard>
  </section>
</template>
