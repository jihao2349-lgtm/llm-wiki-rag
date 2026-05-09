<script setup lang="ts">
import { onMounted, reactive, ref } from "vue"
import {
  NAlert,
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
import { embeddingApi, settingsApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import type { LlmSettings } from "../types"

const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const testingEmbed = ref(false)
const embeddingProvider = ref("dashscope")
const errorMessage = ref("")
const successMessage = ref("")

const form = reactive<LlmSettings>({
  vaultId: 1,
  provider: "OpenAI-compatible",
  baseUrl: "",
  apiKey: "",
  apiKeyMasked: "",
  model: "",
  maxContextSize: 32000,
  temperature: 0.2,
  outputLanguage: "Chinese",
  embeddingEnabled: false,
  embeddingBaseUrl: "https://dashscope.aliyuncs.com/compatible-mode/v1",
  embeddingApiKey: "",
  embeddingApiKeyMasked: "",
  embeddingModel: "text-embedding-v4",
  embeddingDimension: 1024,
  embeddingBatchSize: 10,
  vectorBackend: "none",
  rerankerEnabled: false,
})

function assignSettings(settings: LlmSettings) {
  Object.assign(form, settings, { apiKey: "", embeddingApiKey: "" })
  if (form.embeddingBaseUrl?.includes("dashscope")) {
    embeddingProvider.value = "dashscope"
  } else if (form.embeddingBaseUrl?.includes("volces.com") || form.embeddingBaseUrl?.includes("ark")) {
    embeddingProvider.value = "doubao"
  } else {
    embeddingProvider.value = "openai-compatible"
  }
}

function setEmbeddingProvider(value: string) {
  embeddingProvider.value = value
  if (value === "dashscope") {
    form.embeddingBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    form.embeddingModel = "text-embedding-v4"
    form.embeddingDimension = 1024
    form.embeddingBatchSize = 10
  } else if (value === "doubao") {
    form.embeddingBaseUrl = "https://ark.cn-beijing.volces.com/api/v3"
    form.embeddingModel = "ep-xxxxxxxxxx-xxxxx"
    form.embeddingDimension = 2048
    form.embeddingBatchSize = 10
  }
}

async function loadSettings() {
  loading.value = true
  errorMessage.value = ""
  try {
    assignSettings(await settingsApi.detail())
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  errorMessage.value = ""
  successMessage.value = ""
  try {
    assignSettings(await settingsApi.update(form))
    successMessage.value = "模型设置已保存"
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    saving.value = false
  }
}

async function testLlm() {
  testing.value = true
  errorMessage.value = ""
  successMessage.value = ""
  try {
    await settingsApi.testLlm(form)
    successMessage.value = "LLM 连通性测试通过"
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    testing.value = false
  }
}

async function testEmbedding() {
  if (!form.embeddingBaseUrl || !form.embeddingModel) return
  testingEmbed.value = true
  errorMessage.value = ""
  successMessage.value = ""
  try {
    const result = await embeddingApi.test({
      baseUrl: form.embeddingBaseUrl,
      apiKey: form.embeddingApiKey || "",
      model: form.embeddingModel,
      dimension: form.embeddingDimension,
    }) as Record<string, unknown>
    if (result?.success) {
      successMessage.value = `Embedding 连通性测试通过，向量维度: ${result.dimension}`
    } else {
      errorMessage.value = String(result?.message ?? "Embedding 测试失败")
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    testingEmbed.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <section class="page-grid">
    <Teleport to="body">
      <div v-if="errorMessage || successMessage" class="toast-bar" :class="errorMessage ? 'toast-bar--error' : 'toast-bar--success'">
        <span>{{ errorMessage || successMessage }}</span>
        <button class="toast-bar__close" @click="errorMessage = ''; successMessage = ''">✕</button>
      </div>
    </Teleport>

    <div class="section-panel">
      <div class="section-toolbar">
        <div>
          <h2>设置</h2>
          <p>配置 v0.1 统一 LLM 调用。API Key 保存后只显示脱敏值。</p>
        </div>
        <NTag :bordered="false" type="info" size="small">
          Vault #{{ form.vaultId ?? 1 }}
        </NTag>
      </div>

      <NForm label-placement="top" :disabled="loading">
        <div class="form-grid">
          <NFormItem label="Provider">
            <NSelect
              v-model:value="form.provider"
              :options="[
                { label: 'OpenAI-compatible', value: 'OpenAI-compatible' },
                { label: 'DashScope', value: 'DashScope' },
              ]"
            />
          </NFormItem>
          <NFormItem label="Model">
            <NInput v-model:value="form.model" placeholder="qwen-plus / gpt-4.1-mini" />
          </NFormItem>
        </div>

        <NFormItem label="Base URL">
          <NInput v-model:value="form.baseUrl" placeholder="https://api.openai.com/v1" />
        </NFormItem>

        <NFormItem label="API Key">
          <NInput
            v-model:value="form.apiKey"
            type="password"
            show-password-on="click"
            :placeholder="form.apiKeyMasked || '保存后仅返回 masked key'"
          />
        </NFormItem>

        <div class="form-grid">
          <NFormItem label="Max Context">
            <NInputNumber v-model:value="form.maxContextSize" :min="4096" :step="1024" />
          </NFormItem>
          <NFormItem label="Temperature">
            <NInputNumber v-model:value="form.temperature" :min="0" :max="2" :step="0.1" />
          </NFormItem>
        </div>

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
          <NFormItem label="Embedding">
            <div class="settings-row" style="width: 100%">
              <div>
                <strong>启用 embedding</strong>
                <span>开启后使用下方向量模型配置进行语义检索。</span>
              </div>
              <NSwitch v-model:value="form.embeddingEnabled" />
            </div>
          </NFormItem>
        </div>

        <div class="embed-section">
          <div class="embed-section__header">
            <div>
              <h3>向量化模型配置</h3>
              <p>配置兼容 OpenAI /v1/embeddings 的向量化服务。</p>
            </div>
            <NTag :bordered="false" type="success" size="small">Embedding</NTag>
          </div>

          <div class="form-grid">
            <NFormItem label="Embedding Provider">
              <NSelect
                :value="embeddingProvider"
                :options="[
                  { label: 'DashScope（阿里云）', value: 'dashscope' },
                  { label: '豆包 Embedding（火山方舟）', value: 'doubao' },
                  { label: '自定义 OpenAI-compatible', value: 'openai-compatible' },
                ]"
                @update:value="setEmbeddingProvider"
              />
            </NFormItem>
            <NFormItem label="Embedding Model">
              <NInput
                v-model:value="form.embeddingModel"
                :placeholder="embeddingProvider === 'doubao' ? '填入接入点 ID，如 ep-20xxxxxx-xxxxx' : 'text-embedding-v4'"
              />
            </NFormItem>
          </div>

          <NFormItem label="Embedding Base URL">
            <NInput
              v-model:value="form.embeddingBaseUrl"
              placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1"
            />
          </NFormItem>

          <NFormItem label="Embedding API Key">
            <NInput
              v-model:value="form.embeddingApiKey"
              type="password"
              show-password-on="click"
              :placeholder="form.embeddingApiKeyMasked || '输入 Embedding API Key'"
            />
          </NFormItem>

          <div class="form-grid">
            <NFormItem label="Embedding Dimension">
              <NInputNumber v-model:value="form.embeddingDimension" :min="64" :max="4096" />
            </NFormItem>
            <NFormItem label="Embedding Batch Size">
              <NInputNumber v-model:value="form.embeddingBatchSize" :min="1" :max="100" />
            </NFormItem>
          </div>

          <NButton secondary :loading="testingEmbed" @click="testEmbedding" style="margin-bottom: 16px">
            <template #icon><AppIcon name="play" /></template>
            测试向量连接
          </NButton>
        </div>

        <NSpace :size="8">
          <NButton type="primary" :loading="saving" @click="saveSettings">
            <template #icon>
              <AppIcon name="save" />
            </template>
            保存模型设置
          </NButton>
          <NButton secondary :loading="testing" @click="testLlm">
            <template #icon>
              <AppIcon name="play" />
            </template>
            测试连通性
          </NButton>
          <NButton tertiary :loading="loading" @click="loadSettings">
            <template #icon>
              <AppIcon name="refresh" />
            </template>
            重新加载
          </NButton>
        </NSpace>
      </NForm>
    </div>
  </section>
</template>

<style scoped>
.toast-bar {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  max-width: 520px;
  white-space: nowrap;
}
.toast-bar--success { background: #10b981; color: #fff; }
.toast-bar--error   { background: #ef4444; color: #fff; }
.toast-bar__close {
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  font-size: 14px;
  opacity: 0.8;
  padding: 0;
  line-height: 1;
}
.toast-bar__close:hover { opacity: 1; }
</style>

<style scoped>
.embed-section {
  margin: 6px 0 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.embed-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.embed-section__header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: #0f172a;
}

.embed-section__header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}
</style>
