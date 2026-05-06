<script setup lang="ts">
import {
  NAlert,
  NButton,
  NDataTable,
  NDivider,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NInputGroup,
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

const urlInput = ref("")
const loading = ref(true)
const actionLoading = ref(false)
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
        { size: "small", secondary: true, onClick: () => ingestSource(row) },
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

async function importUrl() {
  if (!urlInput.value) return
  actionLoading.value = true
  actionMessage.value = ""
  errorMessage.value = ""
  try {
    await sourceApi.importUrl(urlInput.value)
    actionMessage.value = "URL 已提交导入"
    await loadSources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    actionLoading.value = false
  }
}

async function uploadSource(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) {
    options.onError()
    return
  }

  try {
    await sourceApi.upload(file)
    options.onFinish()
    await loadSources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
    options.onError()
  }
}

async function ingestSource(source: SourceDocument) {
  actionLoading.value = true
  actionMessage.value = ""
  errorMessage.value = ""
  try {
    await sourceApi.ingest(source.id)
    actionMessage.value = `${source.title} 已加入摄入队列`
    await loadSources()
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    actionLoading.value = false
  }
}

async function parseSource() {
  if (!selectedSource.value) return
  actionLoading.value = true
  errorMessage.value = ""
  try {
    await sourceApi.parse(selectedSource.value.id)
    await loadPreview(selectedSource.value)
    actionMessage.value = "解析已触发"
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadSources)
</script>

<template>
  <section class="page-grid sources-grid">
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>
    <NAlert v-else-if="actionMessage" type="success" :bordered="false">
      {{ actionMessage }}
    </NAlert>

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

      <NDivider />

      <NForm label-placement="top">
        <NFormItem label="网页 URL">
          <NInputGroup>
            <NInput v-model:value="urlInput" placeholder="https://..." />
            <NButton type="primary" :loading="actionLoading" @click="importUrl">
              <template #icon>
                <AppIcon name="link" />
              </template>
              抓取
            </NButton>
          </NInputGroup>
        </NFormItem>
      </NForm>
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
          <NButton secondary :loading="actionLoading" @click="parseSource">
            <template #icon>
              <AppIcon name="file" />
            </template>
            重新解析
          </NButton>
          <NButton type="primary" :loading="actionLoading" @click="ingestSource(selectedSource)">
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
