<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from "vue"
import { NAlert, NButton, NEmpty, NInput, NSpace, NSpin, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { wikiApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName, WikiPage, WikiTreeNode } from "../types"

// ---- Resizable panel ----
const TREE_MIN = 180
const TREE_MAX = 600
const treeWidth = ref(280)
const isDragging = ref(false)

function onDividerMousedown(e: MouseEvent) {
  e.preventDefault()
  isDragging.value = true
  const startX = e.clientX
  const startWidth = treeWidth.value

  function onMousemove(ev: MouseEvent) {
    const delta = ev.clientX - startX
    treeWidth.value = Math.min(TREE_MAX, Math.max(TREE_MIN, startWidth + delta))
  }
  function onMouseup() {
    isDragging.value = false
    window.removeEventListener("mousemove", onMousemove)
    window.removeEventListener("mouseup", onMouseup)
  }
  window.addEventListener("mousemove", onMousemove)
  window.addEventListener("mouseup", onMouseup)
}

onUnmounted(() => { isDragging.value = false })

// ---- Wiki data ----
const loading = ref(true)
const pageLoading = ref(false)
const errorMessage = ref("")
const treeData = ref<WikiTreeNode[]>([])
const flatPages = ref<WikiPage[]>([])
const selectedPath = ref("")
const selectedPage = ref<WikiPage>()
const filterKeyword = ref("")
const isSearchMode = ref(false)
const expandedDirs = ref<Set<string>>(new Set())

interface DisplayItem {
  node: WikiTreeNode
  depth: number
}

function buildDisplayItems(nodes: WikiTreeNode[], depth: number): DisplayItem[] {
  const result: DisplayItem[] = []
  for (const node of nodes) {
    result.push({ node, depth })
    if (node.type === "directory" && expandedDirs.value.has(node.path)) {
      result.push(...buildDisplayItems(node.children ?? [], depth + 1))
    }
  }
  return result
}

const displayItems = computed(() => {
  if (isSearchMode.value) return []
  return buildDisplayItems(treeData.value, 0)
})

function toggleDir(path: string) {
  if (expandedDirs.value.has(path)) {
    expandedDirs.value.delete(path)
  } else {
    expandedDirs.value.add(path)
  }
  expandedDirs.value = new Set(expandedDirs.value)
}

function expandAll(nodes: WikiTreeNode[]) {
  for (const node of nodes) {
    if (node.type === "directory") {
      expandedDirs.value.add(node.path)
      expandAll(node.children ?? [])
    }
  }
}

function flattenTree(nodes: WikiTreeNode[]): WikiPage[] {
  return nodes.flatMap((node) => {
    if (node.type === "directory") return flattenTree(node.children ?? [])
    return [{ path: node.path, title: node.title, type: "source", updatedAt: "未知", frontmatter: "", body: "" } satisfies WikiPage]
  })
}

const totalPageCount = computed(() => flattenTree(treeData.value).length)

async function loadTree() {
  loading.value = true
  isSearchMode.value = false
  errorMessage.value = ""
  try {
    const tree = await wikiApi.tree()
    treeData.value = tree
    expandedDirs.value = new Set()
    expandAll(tree)
    const allPages = flattenTree(tree)
    if (allPages[0] && !selectedPath.value) await selectPage(allPages[0].path)
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
  isSearchMode.value = true
  errorMessage.value = ""
  try {
    flatPages.value = await wikiApi.search(filterKeyword.value)
    if (flatPages.value[0]) await selectPage(flatPages.value[0].path)
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
  <section
    class="page-grid wiki-grid"
    :style="{ gridTemplateColumns: `${treeWidth}px 4px minmax(0, 1fr)` }"
    :class="{ 'wiki-grid--dragging': isDragging }"
  >
    <NAlert v-if="errorMessage" type="error" :bordered="false" style="grid-column: 1 / -1">
      {{ errorMessage }}
    </NAlert>

    <aside class="section-panel wiki-tree">
      <div class="section-toolbar">
        <div>
          <h2>wiki/</h2>
          <p>{{ totalPageCount }} 个页面 · 兼容 Obsidian wikilink</p>
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
      <NEmpty v-else-if="!isSearchMode && displayItems.length === 0" description="暂无 Wiki 页面" />
      <NEmpty v-else-if="isSearchMode && flatPages.length === 0" description="未找到匹配页面" />

      <!-- 搜索结果：平铺列表 -->
      <template v-else-if="isSearchMode">
        <button
          v-for="page in flatPages"
          :key="page.path"
          class="file-row"
          :class="{ active: selectedPath === page.path }"
          type="button"
          @click="selectPage(page.path)"
        >
          <AppIcon name="file" />
          <span>{{ page.path }}</span>
        </button>
      </template>

      <!-- 目录树 -->
      <div v-else class="wiki-file-tree">
        <template v-for="item in displayItems" :key="item.node.path">
          <!-- 目录行 -->
          <button
            v-if="item.node.type === 'directory'"
            class="tree-dir-row"
            type="button"
            :style="{ paddingLeft: `${item.depth * 14 + 8}px` }"
            @click="toggleDir(item.node.path)"
          >
            <span class="tree-chevron" :class="{ open: expandedDirs.has(item.node.path) }">
              <svg width="10" height="10" viewBox="0 0 10 10" fill="currentColor">
                <path d="M3 2l4 3-4 3V2z"/>
              </svg>
            </span>
            <AppIcon name="folder" :size="14" />
            <span>{{ item.node.title }}</span>
          </button>

          <!-- 文件行 -->
          <button
            v-else
            class="tree-file-row"
            :class="{ active: selectedPath === item.node.path }"
            type="button"
            :style="{ paddingLeft: `${item.depth * 14 + 8}px` }"
            @click="selectPage(item.node.path)"
          >
            <AppIcon name="file" :size="14" />
            <span>{{ item.node.title }}</span>
          </button>
        </template>
      </div>
    </aside>

    <div class="wiki-divider" @mousedown="onDividerMousedown">
      <div class="wiki-divider__handle" />
    </div>

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
