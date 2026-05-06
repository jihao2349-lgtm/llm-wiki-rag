<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue"
import { NAlert, NButton, NEmpty, NInput, NSpace, NSpin, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { wikiApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, WikiPage, WikiTreeNode } from "../types"

const loading = ref(true)
const pageLoading = ref(false)
const errorMessage = ref("")
const pages = ref<WikiPage[]>([])
const selectedPath = ref("")
const selectedPage = ref<WikiPage>()
const filterKeyword = ref("")

const filteredPages = computed(() => {
  if (!filterKeyword.value) return pages.value
  const kw = filterKeyword.value.toLowerCase()
  return pages.value.filter(
    (p) => p.path.toLowerCase().includes(kw) || p.title.toLowerCase().includes(kw),
  )
})

function flattenTree(nodes: WikiTreeNode[]): WikiPage[] {
  return nodes.flatMap((node) => {
    if (node.type === "directory") return flattenTree(node.children ?? [])
    return [
      {
        path: node.path,
        title: node.title,
        type: "source",
        updatedAt: "未知",
        frontmatter: "",
        body: "",
      } satisfies WikiPage,
    ]
  })
}

async function loadTree() {
  loading.value = true
  errorMessage.value = ""
  try {
    const tree = await wikiApi.tree()
    pages.value = flattenTree(tree)
    if (pages.value[0]) await selectPage(pages.value[0].path)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function selectPage(path: string) {
  selectedPath.value = path
  pageLoading.value = true
  errorMessage.value = ""
  try {
    selectedPage.value = await wikiApi.page(path)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    pageLoading.value = false
  }
}

async function searchPages() {
  if (!filterKeyword.value) {
    await loadTree()
    return
  }
  loading.value = true
  errorMessage.value = ""
  try {
    pages.value = await wikiApi.search(filterKeyword.value)
    if (pages.value[0]) await selectPage(pages.value[0].path)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function openInObsidian() {
  if (!selectedPage.value) return
  try {
    await wikiApi.open(selectedPage.value.path)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  }
}

watch(filterKeyword, (value, oldValue) => {
  if (!value && oldValue) void loadTree()
})

onMounted(loadTree)
</script>

<template>
  <section class="page-grid wiki-grid">
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>

    <aside class="section-panel wiki-tree">
      <div class="section-toolbar">
        <div>
          <h2>wiki/</h2>
          <p>{{ filteredPages.length }} 个页面 · 兼容 Obsidian wikilink</p>
        </div>
      </div>
      <NInput v-model:value="filterKeyword" clearable placeholder="搜索页面" @keyup.enter="searchPages">
        <template #prefix>
          <AppIcon name="search" />
        </template>
      </NInput>
      <NButton secondary :loading="loading" @click="searchPages">
        <template #icon>
          <AppIcon name="search" />
        </template>
        搜索
      </NButton>
      <NSpin v-if="loading" />
      <NEmpty v-else-if="filteredPages.length === 0" description="暂无 Wiki 页面" />
      <button
        v-else
        v-for="page in filteredPages"
        :key="page.path"
        class="file-row"
        :class="{ active: selectedPath === page.path }"
        type="button"
        @click="selectPage(page.path)"
      >
        <AppIcon name="file" />
        <span>{{ page.path }}</span>
      </button>
    </aside>

    <main v-if="selectedPage" class="section-panel wiki-preview">
      <div class="section-toolbar">
        <div>
          <h2>{{ selectedPage.title }}</h2>
          <p>{{ selectedPage.path }} · 更新于 {{ selectedPage.updatedAt }}</p>
        </div>
        <NSpace :size="8" align="center">
          <span
            v-if="selectedPage.modality"
            :class="`modality-chip modality-chip--${selectedPage.modality}`"
          >
            <AppIcon
              :name="(modalityIcon(selectedPage.modality) as IconName)"
              :size="12"
            />
            {{ modalityLabel(selectedPage.modality) }}
          </span>
          <NTag round :bordered="false" type="info" size="small">{{ selectedPage.type }}</NTag>
          <NButton secondary @click="openInObsidian">
            <template #icon>
              <AppIcon name="external" />
            </template>
            在 Obsidian 打开
          </NButton>
        </NSpace>
      </div>

      <NSpin v-if="pageLoading" />
      <pre v-else class="frontmatter">{{ selectedPage.frontmatter }}</pre>
      <article class="markdown-body">
        <pre>{{ selectedPage.body }}</pre>
      </article>
    </main>
    <main v-else class="section-panel wiki-preview">
      <NEmpty description="请选择一个 Wiki 页面" />
    </main>
  </section>
</template>
