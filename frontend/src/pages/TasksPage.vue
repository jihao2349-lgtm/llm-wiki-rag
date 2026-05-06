<script setup lang="ts">
import { computed } from "vue"
import { NButton, NProgress, NSpace, NTimeline, NTimelineItem } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import StatusTag from "../components/StatusTag.vue"
import { tasks } from "../mock-data"
import { taskStatusLabel } from "../utils/status"

const failedTasks = computed(() => tasks.filter((task) => task.status === "Failed").length)
</script>

<template>
  <section class="page-grid tasks-grid">
    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>摄入队列</h2>
          <p>任务默认按 Vault 串行执行，避免多个任务同时写入。</p>
        </div>
        <NButton secondary :disabled="failedTasks === 0">
          <template #icon>
            <AppIcon name="refresh" />
          </template>
          重试失败 ({{ failedTasks }})
        </NButton>
      </div>

      <div class="task-list">
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
            <NButton size="small" tertiary>
              <template #icon>
                <AppIcon name="x" />
              </template>
              取消
            </NButton>
            <NButton size="small" secondary>
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
