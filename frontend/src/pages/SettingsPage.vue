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
  embeddingModel: "text-embedding-v3",
  embeddingDimension: 1024,
  embeddingBatchSize: 25,
  vectorBackend: "none",
  rerankerEnabled: false,
})

function assignSettings(settings: LlmSettings) {
  Object.assign(form, settings, { apiKey: "", embeddingApiKey: "" })
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
      apiKey: form.embeddingApiKey || form.embeddingApiKeyMasked || "",
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
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>
    <NAlert v-else-if="successMessage" type="success" :bordered="false">
      {{ successMessage }}
    </NAlert>

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
                <span>v0.1 默认关闭，开启后由后端配置决定是否生效。</span>
              </div>
              <NSwitch v-model:value="form.embeddingEnabled" />
            </div>
          </NFormItem>
        </div>

        <!-- Embedding 配置区块 -->
        <template v-if="form.embeddingEnabled">
          <div class="embed-section-title">Embedding 配置（向量检索）</div>

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
              :placeholder="form.embeddingApiKeyMasked || '输入 API Key'"
            />
          </NFormItem>

          <div class="form-grid">
            <NFormItem label="Embedding Model">
              <NInput v-model:value="form.embeddingModel" placeholder="text-embedding-v3" />
            </NFormItem>
            <NFormItem label="Dimension">
              <NInputNumber v-model:value="form.embeddingDimension" :min="64" :max="4096" />
            </NFormItem>
            <NFormItem label="Batch Size">
              <NInputNumber v-model:value="form.embeddingBatchSize" :min="1" :max="100" />
            </NFormItem>
          </div>

          <NButton secondary :loading="testingEmbed" @click="testEmbedding" style="margin-bottom: 16px">
            <template #icon><AppIcon name="play" /></template>
            测试 Embedding 连通性
          </NButton>
        </template>

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
.embed-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 8px 0 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(255,255,255,0.06);
}
</style>
