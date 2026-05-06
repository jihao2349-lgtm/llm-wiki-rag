<script setup lang="ts">
import {
  NButton,
  NDataTable,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NInputGroup,
  NSpace,
  NUpload,
  NUploadDragger,
  type DataTableColumns,
} from "naive-ui"
import { h, ref } from "vue"
import AppIcon from "../components/AppIcon.vue"
import StatusTag from "../components/StatusTag.vue"
import { sources } from "../mock-data"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, SourceDocument } from "../types"

const urlInput = ref("https://example.com/research/agent-memory")
const selectedSource = ref(sources[0])

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
    render() {
      return h(
        NButton,
        { size: "small", secondary: true },
        {
          icon: () => h(AppIcon, { name: "play" }),
          default: () => "摄入",
        },
      )
    },
  },
]
</script>

<template>
  <section class="page-grid sources-grid">
    <div class="import-panel">
      <div class="panel-block">
        <h2>多模态导入</h2>
        <p>支持 PDF、Office、Markdown、HTML、网页 URL、图片、音频、视频，统一进入 raw/ 目录。</p>
      </div>

      <NUpload multiple>
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
            <NButton type="primary">
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
        <NButton secondary>
          <template #icon>
            <AppIcon name="play" />
          </template>
          批量摄入
        </NButton>
      </div>
      <NDataTable
        :columns="sourceColumns"
        :data="sources"
        :bordered="false"
        :pagination="{ pageSize: 6 }"
      />
    </div>

    <div class="preview-panel">
      <div class="section-toolbar">
        <div>
          <h2>解析预览</h2>
          <p>{{ selectedSource.extractedTextPath }}</p>
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
        <p>
          Agent memory systems usually combine short-term conversational state,
          long-term semantic memory, and episodic traces. A local-first vault keeps
          the durable layer inspectable and recoverable.
        </p>
        <NSpace :size="8">
          <NButton secondary>
            <template #icon>
              <AppIcon name="file" />
            </template>
            查看原文
          </NButton>
          <NButton type="primary">
            <template #icon>
              <AppIcon name="spark" />
            </template>
            AI 摄入
          </NButton>
        </NSpace>
      </article>
    </div>
  </section>
</template>
