<script setup lang="ts">
import {
  AlertTriangle,
  Archive,
  AudioLines,
  Bot,
  Check,
  Code2,
  Copy,
  Database,
  ExternalLink,
  FileText,
  Folder,
  Globe,
  Image as ImageIcon,
  KeyRound,
  LayoutDashboard,
  Link,
  ListChecks,
  MessageSquare,
  Play,
  Plug,
  Plus,
  RefreshCcw,
  Save,
  Search,
  Settings,
  Sparkles,
  Trash2,
  Upload,
  Video,
  X,
} from "lucide-vue-next"
import type { Component } from "vue"
import type { IconName } from "../types"

defineProps<{
  name: IconName
  size?: number
}>()

const iconComponents: Record<IconName, Component> = {
  alert: AlertTriangle,
  archive: Archive,
  audio: AudioLines,
  bot: Bot,
  check: Check,
  code: Code2,
  copy: Copy,
  database: Database,
  external: ExternalLink,
  file: FileText,
  folder: Folder,
  globe: Globe,
  image: ImageIcon,
  key: KeyRound,
  layout: LayoutDashboard,
  link: Link,
  list: ListChecks,
  message: MessageSquare,
  mcp: Plug,
  play: Play,
  plus: Plus,
  refresh: RefreshCcw,
  save: Save,
  search: Search,
  settings: Settings,
  spark: Sparkles,
  trash: Trash2,
  upload: Upload,
  video: Video,
  x: X,
}
</script>

<template>
  <component
    :is="iconComponents[name]"
    class="app-icon"
    :size="size ?? 18"
    :stroke-width="1.8"
    aria-hidden="true"
  />
</template>
