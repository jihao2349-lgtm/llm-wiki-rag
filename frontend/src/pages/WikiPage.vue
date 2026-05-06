<script setup lang="ts">
import { computed, ref } from "vue"
import { NButton, NInput, NSpace, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { wikiPages } from "../mock-data"
import { modalityIcon, modalityLabel } from "../utils/status"
import type { IconName } from "../types"

const selectedPath = ref("wiki/concepts/agent-memory.md")
const filterKeyword = ref("")

const filteredPages = computed(() => {
  if (!filterKeyword.value) return wikiPages
  const kw = filterKeyword.value.toLowerCase()
  return wikiPages.filter(
    (p) => p.path.toLowerCase().includes(kw) || p.title.toLowerCase().includes(kw),
  )
})

const selectedPage = computed(
  () => wikiPages.find((page) => page.path === selectedPath.value) ?? wikiPages[0],
)
</script>

<template>
  <section class="page-grid wiki-grid">
    <aside class="section-panel wiki-tree">
      <div class="section-toolbar">
        <div>
          <h2>wiki/</h2>
          <p>{{ filteredPages.length }} 个页面 · 兼容 Obsidian wikilink</p>
        </div>
      </div>
      <NInput v-model:value="filterKeyword" clearable placeholder="过滤页面">
        <template #prefix>
          <AppIcon name="search" />
        </template>
      </NInput>
      <button
        v-for="page in filteredPages"
        :key="page.path"
        class="file-row"
        :class="{ active: selectedPath === page.path }"
        type="button"
        @click="selectedPath = page.path"
      >
        <AppIcon name="file" />
        <span>{{ page.path }}</span>
      </button>
    </aside>

    <main class="section-panel wiki-preview">
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
          <NButton secondary>
            <template #icon>
              <AppIcon name="external" />
            </template>
            在 Obsidian 打开
          </NButton>
        </NSpace>
      </div>

      <pre class="frontmatter">{{ selectedPage.frontmatter }}</pre>
      <article class="markdown-body">
        <pre>{{ selectedPage.body }}</pre>
      </article>
    </main>
  </section>
</template>
