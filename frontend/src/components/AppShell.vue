<script setup lang="ts">
import { computed, h, ref } from "vue"
import {
  NButton,
  NConfigProvider,
  NInput,
  NLayout,
  NLayoutContent,
  NLayoutHeader,
  NLayoutSider,
  NMenu,
  NSpace,
  NTooltip,
  type GlobalTheme,
  type MenuOption,
} from "naive-ui"
import AppIcon from "./AppIcon.vue"
import { vaultProject } from "../mock-data"
import type { IconName, PageKey } from "../types"

const props = defineProps<{
  activePage: PageKey
  darkTheme: GlobalTheme
  pageTitle: string
  pageSubtitle?: string
}>()

const emit = defineEmits<{
  "update:activePage": [value: PageKey]
}>()

const collapsed = ref(false)

const activePageModel = computed({
  get: () => props.activePage,
  set: (value: PageKey) => emit("update:activePage", value),
})

function renderIcon(name: IconName) {
  return () => h(AppIcon, { name })
}

const navOptions: MenuOption[] = [
  { key: "dashboard", label: "工作台", icon: renderIcon("layout") },
  { key: "sources", label: "资料导入", icon: renderIcon("upload") },
  { key: "tasks", label: "摄入队列", icon: renderIcon("list") },
  { key: "chat", label: "AI 对话", icon: renderIcon("message") },
  { key: "wiki", label: "Wiki 预览", icon: renderIcon("file") },
  { key: "embedding", label: "向量管理", icon: renderIcon("database") },
]

const developerOptions: MenuOption[] = [
  { key: "apiconsole", label: "API Console", icon: renderIcon("code") },
  { key: "settings", label: "设置", icon: renderIcon("settings") },
]
</script>

<template>
  <NLayout has-sider class="app-shell">
    <NConfigProvider :theme="darkTheme">
      <NLayoutSider
        bordered
        class="app-sider"
        :width="252"
        :collapsed-width="64"
        :collapsed="collapsed"
        collapse-mode="width"
        :native-scrollbar="true"
        :show-trigger="false"
      >
        <div class="sider-inner">
          <div class="brand" :class="{ 'brand--collapsed': collapsed }">
            <div class="brand-mark" @click="collapsed = !collapsed" style="cursor:pointer">
              <AppIcon name="spark" :size="20" />
            </div>
            <template v-if="!collapsed">
              <div class="brand-text">
                <strong>AI Wiki</strong>
                <span>Obsidian 编译层</span>
              </div>
              <button class="collapse-btn" @click="collapsed = true" title="收起侧边栏">
                <AppIcon name="layout" :size="15" />
              </button>
            </template>
          </div>

          <template v-if="!collapsed">
            <div class="sider-scroll">
              <div class="side-section">
                <span class="side-section-label">主菜单</span>
                <NMenu
                  v-model:value="activePageModel"
                  :options="navOptions"
                  class="side-menu"
                  :indent="14"
                />
              </div>

              <div class="side-section">
                <span class="side-section-label">开发者</span>
                <NMenu
                  v-model:value="activePageModel"
                  :options="developerOptions"
                  class="side-menu"
                  :indent="14"
                />
              </div>
            </div>

            <div class="vault-summary">
              <div class="vault-summary__label">
                <span class="vault-summary__dot" />
                当前 Vault
              </div>
              <strong>{{ vaultProject.name }}</strong>
              <span>{{ vaultProject.path }}</span>
            </div>
          </template>

          <template v-else>
            <div class="collapsed-nav">
              <NTooltip v-for="item in navOptions" :key="item.key" placement="right">
                <template #trigger>
                  <button
                    class="collapsed-nav-btn"
                    :class="{ active: activePageModel === item.key }"
                    @click="activePageModel = (item.key as PageKey)"
                  >
                    <component :is="item.icon!()" />
                  </button>
                </template>
                {{ item.label }}
              </NTooltip>
              <div class="collapsed-divider" />
              <NTooltip v-for="item in developerOptions" :key="item.key" placement="right">
                <template #trigger>
                  <button
                    class="collapsed-nav-btn"
                    :class="{ active: activePageModel === item.key }"
                    @click="activePageModel = (item.key as PageKey)"
                  >
                    <component :is="item.icon!()" />
                  </button>
                </template>
                {{ item.label }}
              </NTooltip>
            </div>
          </template>
        </div>
      </NLayoutSider>
    </NConfigProvider>

    <NLayout class="main-layout">
      <NLayoutHeader class="topbar" bordered>
        <div>
          <span class="eyebrow">
            <AppIcon name="spark" :size="13" />
            AI Obsidian Wiki
          </span>
          <h1>{{ pageTitle }}</h1>
        </div>
        <NSpace align="center" :size="10" class="top-actions">
          <NInput clearable placeholder="搜索页面、资料、任务" class="top-search">
            <template #prefix>
              <AppIcon name="search" />
            </template>
          </NInput>
          <NButton type="primary" @click="activePageModel = 'sources'">
            <template #icon>
              <AppIcon name="upload" />
            </template>
            导入资料
          </NButton>
        </NSpace>
      </NLayoutHeader>

      <NLayoutContent class="content">
        <slot />
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>
