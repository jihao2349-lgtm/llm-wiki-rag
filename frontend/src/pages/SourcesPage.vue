<script setup lang="ts">
import {
  NAlert,
  NButton,
  NDataTable,
  NEmpty,
  NSpace,
  NSpin,
  NUpload,
  NUploadDragger,
  type DataTableColumns,
  type UploadCustomRequestOptions,
} from "naive-ui"
import { h, onMounted, ref } from "vue"
import AppIcon from "../components/AppIcon.vue"
import StatusTag from "../components/StatusTag.vue"
import { sourceApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, SourceDocument, SourcePreview } from "../types"

const loading = ref(true)
const uploadLoading = ref(false)
const ingestLoadingId = ref("")   // 正在摄入的 sourceId
const parseLoading = ref(false)
const errorMessage = ref("")
const actionMessage = ref("")
const sources = ref<SourceDocument[]>([])
const selectedSource = ref<SourceDocument>()
const selectedPreview = ref<SourcePreview>()

const sourceColumns: DataTableColumns<SourceDocument> = [
  {
    title: "资料",
    key: "title",
    render(row) {
      return h(
        "button",
        {
          class: "link-button",
          type: "button",
          onClick: () => {
            selectedSource.value = row
          },
        },
        row.title,
      )
    },
  },
  {
    title: "模态",
    key: "modality",
    width: 100,
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
  { title: "类型", key: "type", width: 90 },
  { title: "大小", key: "size", width: 90 },
  {
    title: "状态",
    key: "status",
    width: 110,
    render(row) {
      return h(StatusTag, { status: row.status })
    },
  },
  { title: "原始位置", key: "originalPath" },
  {
    title: "操作",
    key: "actions",
    width: 110,
    render(row) {
      return h(
        NButton,
        {
            size: "small",
            secondary: true,
            loading: ingestLoadingId.value === row.id,
            disabled: !!ingestLoadingId.value,
            onClick: () => ingestSource(row),
          },
        {
          icon: () => h(AppIcon, { name: "play" }),
          default: () => "摄入",
        },
      )
    },
  },
]

async function loadSources() {
  loading.value = true
  errorMessage.value = ""
  try {
    const page = await sourceApi.page()
    sources.value = page.records
    selectedSource.value = page.records[0]
    if (selectedSource.value) await loadPreview(selectedSource.value)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadPreview(source: SourceDocument) {
  selectedSource.value = source
  selectedPreview.value = undefined
  try {
    selectedPreview.value = await sourceApi.preview(source.id)
  } catch {
    selectedPreview.value = {
      sourceId: source.id,
      title: source.title,
      extractedTextPath: source.extractedTextPath,
      content: source.summary,
    }
  }
}

async function uploadSource(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) {
    options.onError()
    return
  }
  uploadLoading.value = true
  try {
    await sourceApi.upload(file)
    options.onFinish()
    await loadSources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    options.onError()
  } finally {
    uploadLoading.value = false
  }
}

async function ingestSource(source: SourceDocument) {
  ingestLoadingId.value = source.id
  actionMessage.value = ""
  errorMessage.value = ""
  try {
    await sourceApi.ingest(source.id)
    // 只更新该条资料的状态，不重新加载整个列表（避免布局抖动）
    const idx = sources.value.findIndex((s) => s.id === source.id)
    if (idx >= 0) sources.value[idx] = { ...sources.value[idx], status: "解析中" }
    actionMessage.value = `「${source.title}」已加入摄入队列，请前往摄入队列查看进度`
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    ingestLoadingId.value = ""
  }
}

async function parseSource() {
  if (!selectedSource.value) return
  parseLoading.value = true
  errorMessage.value = ""
  try {
    await sourceApi.parse(selectedSource.value.id)
    await loadPreview(selectedSource.value)
    actionMessage.value = "解析已触发"
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    parseLoading.value = false
  }
}

onMounted(loadSources)
</script>

<template>
  <!-- Alert 放在 grid 外，避免作为 grid 子元素撑乱布局 -->
  <NAlert v-if="errorMessage" type="error" :bordered="false" style="margin-bottom: 12px">
    {{ errorMessage }}
  </NAlert>
  <NAlert v-else-if="actionMessage" type="success" :bordered="false" style="margin-bottom: 12px">
    {{ actionMessage }}
  </NAlert>

  <section class="page-grid sources-grid">
    <div class="import-panel">
      <div class="panel-block">
        <h2>资料导入</h2>
        <p>支持 PDF、Office、Markdown、HTML 和网页 URL，统一进入 raw/ 目录。</p>
      </div>

      <NUpload multiple :custom-request="uploadSource" :show-file-list="false">
        <NUploadDragger>
          <div class="upload-dragger">
            <AppIcon name="upload" :size="32" />
            <strong>拖放文件到这里</strong>
            <span>原始资料保存到 raw/sources 或 raw/assets，解析失败也保留原文件。</span>
          </div>
        </NUploadDragger>
      </NUpload>

      <!-- URL 抓取功能暂未开放，已隐藏 -->
    </div>

    <div class="source-table-panel">
      <div class="section-toolbar">
        <div>
          <h2>资料列表</h2>
          <p>解析完成后可手动触发 AI 摄入，或加入串行队列。</p>
        </div>
        <NButton secondary :loading="loading" @click="loadSources">
          <template #icon>
            <AppIcon name="refresh" />
          </template>
          刷新
        </NButton>
      </div>
      <NSpin v-if="loading" />
      <NDataTable
        v-else
        :columns="sourceColumns"
        :data="sources"
        :bordered="false"
        :pagination="{ pageSize: 6 }"
      />
    </div>

    <div v-if="selectedSource" class="preview-panel">
      <div class="section-toolbar">
        <div>
          <h2>解析预览</h2>
          <p>{{ selectedPreview?.extractedTextPath || selectedSource.extractedTextPath }}</p>
        </div>
        <NSpace :size="8" align="center">
          <span :class="`modality-chip modality-chip--${selectedSource.modality}`">
            <AppIcon :name="(modalityIcon(selectedSource.modality) as IconName)" :size="12" />
            {{ modalityLabel(selectedSource.modality) }}
          </span>
          <StatusTag :status="selectedSource.status" />
        </NSpace>
      </div>
      <article class="doc-preview">
        <h3>{{ selectedSource.title }}</h3>
        <p>{{ selectedSource.summary }}</p>
        <h3>写入目标</h3>
        <p>{{ selectedSource.targetPage }}</p>
        <h3>解析结果片段</h3>
        <p>{{ selectedPreview?.content || "暂无解析预览" }}</p>
        <NSpace :size="8">
          <NButton secondary :loading="parseLoading" @click="parseSource">
            <template #icon>
              <AppIcon name="file" />
            </template>
            重新解析
          </NButton>
          <NButton
            type="primary"
            :loading="ingestLoadingId === selectedSource.id"
            :disabled="!!ingestLoadingId"
            @click="ingestSource(selectedSource)"
          >
            <template #icon>
              <AppIcon name="spark" />
            </template>
            AI 摄入
          </NButton>
        </NSpace>
      </article>
    </div>
    <div v-else class="preview-panel">
      <NEmpty description="暂无资料，先上传文件或导入 URL" />
    </div>
  </section>
</template>

