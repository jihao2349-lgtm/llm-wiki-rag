<script setup lang="ts">
import { computed, ref } from "vue"
import { NConfigProvider, darkTheme, type GlobalThemeOverrides } from "naive-ui"
import AppShell from "./components/AppShell.vue"
import ApiConsolePage from "./pages/ApiConsolePage.vue"
import ChatPage from "./pages/ChatPage.vue"
import DashboardPage from "./pages/DashboardPage.vue"
import SettingsPage from "./pages/SettingsPage.vue"
import SourcesPage from "./pages/SourcesPage.vue"
import TasksPage from "./pages/TasksPage.vue"
import WikiPage from "./pages/WikiPage.vue"
import type { PageKey } from "./types"

const activePage = ref<PageKey>("dashboard")

const pageTitles: Record<PageKey, string> = {
  dashboard: "工作台",
  sources: "资料导入",
  tasks: "摄入队列",
  chat: "AI 对话",
  wiki: "Wiki 预览",
  apiconsole: "API Console",
  settings: "设置",
}

const pageSubtitles: Record<PageKey, string> = {
  dashboard: "查看 Vault 状态、生产链路与最近资料。",
  sources: "上传文件、抓取网页，多模态资料一体管理。",
  tasks: "AI 摄入串行执行，失败可重试，进度可追溯。",
  chat: "基于 Vault 检索答案，引用源可点击溯源。",
  wiki: "预览 wiki/ 目录下生成的 Markdown 页面。",
  apiconsole: "测试 RAG API、查看调用日志、配置 MCP Server。",
  settings: "Vault、模型、API Key、MCP 与安全配置。",
}

const currentPageTitle = computed(() => pageTitles[activePage.value])
const currentPageSubtitle = computed(() => pageSubtitles[activePage.value])

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: "#0D9488",
    primaryColorHover: "#0F766E",
    primaryColorPressed: "#115E59",
    primaryColorSuppl: "#14B8A6",
    infoColor: "#6366F1",
    infoColorHover: "#4F46E5",
    successColor: "#10B981",
    warningColor: "#F59E0B",
    errorColor: "#EF4444",
    borderRadius: "10px",
    borderRadiusSmall: "8px",
    fontFamily:
      '"Inter", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    fontWeightStrong: "600",
  },
  Card: {
    borderRadius: "12px",
    paddingMedium: "20px",
  },
  Button: {
    borderRadiusMedium: "10px",
    heightMedium: "36px",
    fontWeight: "500",
  },
  DataTable: {
    thColor: "#F8FAFC",
    thFontWeight: "600",
    thTextColor: "#475569",
    tdColorHover: "#F8FAFC",
    borderRadius: "10px",
  },
  Input: {
    borderRadius: "10px",
  },
  Tag: {
    borderRadius: "999px",
  },
  Progress: {
    railColor: "#EEF2F7",
  },
  Menu: {
    itemTextColor: "#94A3B8",
    itemTextColorHover: "#F1F5F9",
    itemTextColorActive: "#F1F5F9",
    itemTextColorChildActive: "#F1F5F9",
    itemIconColor: "#94A3B8",
    itemIconColorHover: "#F1F5F9",
    itemIconColorActive: "#5EEAD4",
    itemIconColorChildActive: "#5EEAD4",
  },
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <AppShell
      v-model:active-page="activePage"
      :dark-theme="darkTheme"
      :page-title="currentPageTitle"
      :page-subtitle="currentPageSubtitle"
    >
      <DashboardPage v-if="activePage === 'dashboard'" />
      <SourcesPage v-else-if="activePage === 'sources'" />
      <TasksPage v-else-if="activePage === 'tasks'" />
      <ChatPage v-else-if="activePage === 'chat'" />
      <WikiPage v-else-if="activePage === 'wiki'" />
      <ApiConsolePage v-else-if="activePage === 'apiconsole'" />
      <SettingsPage v-else />
    </AppShell>
  </NConfigProvider>
</template>
