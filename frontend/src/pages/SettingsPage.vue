<script setup lang="ts">
import { computed, reactive, ref } from "vue"
import {
  NButton,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
} from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { apiKeys, llmSettings, mcpConfigSample, vaultProject } from "../mock-data"

type SettingsTab = "general" | "model" | "apikeys" | "mcp" | "security"

const activeTab = ref<SettingsTab>("general")

const tabs: Array<{ key: SettingsTab; label: string }> = [
  { key: "general", label: "通用" },
  { key: "model", label: "模型" },
  { key: "apikeys", label: "API Keys" },
  { key: "mcp", label: "MCP Server" },
  { key: "security", label: "索引与安全" },
]

const form = reactive({ ...llmSettings, vaultPath: vaultProject.path })

const vectorBackendOptions = [
  { label: "不启用", value: "none" },
  { label: "PostgreSQL + pgvector", value: "pgvector" },
  { label: "Qdrant", value: "qdrant" },
]

const keys = reactive([...apiKeys])

const usagePercent = (used: number, quota: number) =>
  Math.min(100, Math.round((used / quota) * 100))

const totalActiveKeys = computed(() => keys.filter((k) => k.enabled).length)
</script>

<template>
  <section class="page-grid">
    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>设置</h2>
          <p>当前 Vault：{{ vaultProject.name }} · {{ totalActiveKeys }} 个启用中的 API Key</p>
        </div>
      </div>

      <div class="tab-bar">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="{ active: activeTab === tab.key }"
          type="button"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- General -->
      <NForm v-if="activeTab === 'general'" label-placement="top">
        <NFormItem label="Vault 路径">
          <NInput v-model:value="form.vaultPath" />
        </NFormItem>
        <div class="form-grid">
          <NFormItem label="输出语言">
            <NSelect
              v-model:value="form.outputLanguage"
              :options="[
                { label: 'Chinese', value: 'Chinese' },
                { label: 'English', value: 'English' },
                { label: 'Auto', value: 'Auto' },
              ]"
            />
          </NFormItem>
          <NFormItem label="主题">
            <NSelect
              :value="'auto'"
              :options="[
                { label: 'Auto', value: 'auto' },
                { label: 'Light', value: 'light' },
                { label: 'Dark', value: 'dark' },
              ]"
            />
          </NFormItem>
        </div>
        <NSpace :size="8">
          <NButton type="primary">
            <template #icon>
              <AppIcon name="check" />
            </template>
            初始化 / 保存
          </NButton>
          <NButton secondary>
            <template #icon>
              <AppIcon name="folder" />
            </template>
            选择目录
          </NButton>
        </NSpace>
      </NForm>

      <!-- Model -->
      <NForm v-else-if="activeTab === 'model'" label-placement="top">
        <div class="form-grid">
          <NFormItem label="Provider">
            <NSelect
              v-model:value="form.provider"
              :options="[
                { label: 'OpenAI-compatible', value: 'OpenAI-compatible' },
                { label: 'DashScope', value: 'DashScope' },
                { label: 'Ollama', value: 'Ollama' },
              ]"
            />
          </NFormItem>
          <NFormItem label="Model">
            <NInput v-model:value="form.model" />
          </NFormItem>
        </div>
        <NFormItem label="Base URL">
          <NInput v-model:value="form.baseUrl" />
        </NFormItem>
        <div class="form-grid">
          <NFormItem label="Max Context">
            <NInputNumber v-model:value="form.maxContextSize" :min="4096" :step="1024" />
          </NFormItem>
          <NFormItem label="Temperature">
            <NInputNumber v-model:value="form.temperature" :min="0" :max="2" :step="0.1" />
          </NFormItem>
        </div>
        <NSpace :size="8">
          <NButton type="primary">
            <template #icon>
              <AppIcon name="save" />
            </template>
            保存模型设置
          </NButton>
          <NButton secondary>
            <template #icon>
              <AppIcon name="play" />
            </template>
            测试连通性
          </NButton>
        </NSpace>
      </NForm>

      <!-- API Keys -->
      <div v-else-if="activeTab === 'apikeys'">
        <div class="section-toolbar">
          <div>
            <h2 style="font-size: 1rem">外部 API Key</h2>
            <p>提供给 Coding Agent、第三方应用、自动化脚本调用 RAG 接口。</p>
          </div>
          <NButton type="primary">
            <template #icon>
              <AppIcon name="plus" />
            </template>
            创建 Key
          </NButton>
        </div>
        <div class="api-key-list">
          <div
            v-for="key in keys"
            :key="key.id"
            class="api-key-row"
            :class="{ disabled: !key.enabled }"
          >
            <div class="api-key-info">
              <div class="api-key-title">
                <AppIcon name="key" :size="16" />
                {{ key.name }}
                <NTag
                  v-if="!key.enabled"
                  :bordered="false"
                  size="small"
                  type="default"
                >
                  已禁用
                </NTag>
                <NTag
                  v-else-if="usagePercent(key.used, key.monthlyQuota) >= 90"
                  :bordered="false"
                  size="small"
                  type="warning"
                >
                  接近上限
                </NTag>
              </div>
              <span class="api-key-prefix">{{ key.prefix }}…</span>
              <div class="api-key-meta">
                <span>权限：<strong>{{ key.scopes.join(", ") }}</strong></span>
                <span>QPS：<strong>{{ key.qpsLimit }}</strong></span>
                <span>
                  本月调用：
                  <strong>
                    {{ key.used.toLocaleString() }} / {{ key.monthlyQuota.toLocaleString() }}
                  </strong>
                </span>
                <span v-if="key.expiresAt">过期：<strong>{{ key.expiresAt }}</strong></span>
              </div>
            </div>
            <div class="api-key-actions">
              <NButton size="small" tertiary>
                <template #icon>
                  <AppIcon name="copy" />
                </template>
                复制
              </NButton>
              <NButton size="small" secondary>
                <template #icon>
                  <AppIcon name="settings" />
                </template>
                编辑
              </NButton>
              <NButton size="small" tertiary type="error">
                <template #icon>
                  <AppIcon name="trash" />
                </template>
                撤销
              </NButton>
            </div>
          </div>
        </div>
      </div>

      <!-- MCP -->
      <div v-else-if="activeTab === 'mcp'">
        <div class="section-toolbar">
          <div>
            <h2 style="font-size: 1rem">MCP Server 配置</h2>
            <p>把以下配置粘贴到 Claude Code、Cursor 的 MCP 配置中，即可调用 Vault 工具。</p>
          </div>
          <NButton secondary>
            <template #icon>
              <AppIcon name="copy" />
            </template>
            复制配置
          </NButton>
        </div>
        <pre class="code-block">{{ mcpConfigSample }}</pre>
        <div class="settings-list" style="margin-top: 16px">
          <div class="settings-row">
            <div>
              <strong>wiki_search</strong>
              <span>关键词 + 向量混合检索，返回 top-k 片段</span>
            </div>
            <NTag :bordered="false" type="success" size="small">已启用</NTag>
          </div>
          <div class="settings-row">
            <div>
              <strong>wiki_read</strong>
              <span>按路径读取单个 Markdown 页面原文</span>
            </div>
            <NTag :bordered="false" type="success" size="small">已启用</NTag>
          </div>
          <div class="settings-row">
            <div>
              <strong>wiki_list</strong>
              <span>按 type 列出 Wiki 页面，支持过滤</span>
            </div>
            <NTag :bordered="false" type="success" size="small">已启用</NTag>
          </div>
          <div class="settings-row">
            <div>
              <strong>wiki_ingest</strong>
              <span>外部投喂内容入摄入队列</span>
            </div>
            <NTag :bordered="false" type="warning" size="small">需授权</NTag>
          </div>
        </div>
      </div>

      <!-- Security -->
      <div v-else-if="activeTab === 'security'">
        <NForm label-placement="top">
          <div class="form-grid">
            <NFormItem label="向量后端">
              <NSelect v-model:value="form.vectorBackend" :options="vectorBackendOptions" />
            </NFormItem>
            <NFormItem label="Embedding 模型">
              <NInput :value="'bge-m3'" />
            </NFormItem>
          </div>
        </NForm>
        <div class="settings-list">
          <div class="settings-row">
            <div>
              <strong>启用向量检索</strong>
              <span>关键词 + 向量混合召回，关闭后退回 v0.1 模式。</span>
            </div>
            <NSwitch v-model:value="form.embeddingEnabled" />
          </div>
          <div class="settings-row">
            <div>
              <strong>启用 Reranker</strong>
              <span>使用 bge-reranker-v2 对召回结果重排序。</span>
            </div>
            <NSwitch v-model:value="form.rerankerEnabled" />
          </div>
          <div class="settings-row">
            <div>
              <strong>写入前保留历史</strong>
              <span>更新 index.md、overview.md 前记录可追踪版本。</span>
            </div>
            <NSwitch :value="true" />
          </div>
          <div class="settings-row">
            <div>
              <strong>Review Queue</strong>
              <span>LLM 不确定的问题进入审核队列，不直接覆盖结论。</span>
            </div>
            <NSwitch :value="true" />
          </div>
          <div class="settings-row">
            <div>
              <strong>SSRF 防护</strong>
              <span>URL 导入禁止内网 IP、localhost、metadata 地址。</span>
            </div>
            <NSwitch :value="true" />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
