<script setup lang="ts">
import { computed, onMounted, ref } from "vue"
import { NAlert, NButton, NDropdown, NEmpty, NInput, NSpace, NSpin, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { chatApi, wikiApi } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import type { ChatMessage, ChatReference, ChatSession } from "../types"

const loading = ref(true)
const streaming = ref(false)
const errorMessage = ref("")
const chatInput = ref("")
const sessions = ref<ChatSession[]>([])
const activeSessionId = ref("")
const messages = ref<ChatMessage[]>([])

const saveOptions = [
  { key: "synthesis", label: "保存为合成观点 (synthesis)" },
  { key: "question", label: "保存为开放问题 (question)" },
]

const chatSuggestions = [
  { question: "这批资料的核心概念是什么？", reason: "适合快速建立 wiki/concepts 页面" },
  { question: "有哪些结论需要继续核验？", reason: "帮助发现证据不足的页面" },
  { question: "哪些页面可以合并成 synthesis？", reason: "适合沉淀跨资料观点" },
]

const references = computed<ChatReference[]>(() =>
  messages.value.flatMap((message) => message.references ?? []),
)

const lastAssistantMessage = computed(() =>
  [...messages.value].reverse().find((message) => message.role === "assistant"),
)

async function loadSessions() {
  loading.value = true
  errorMessage.value = ""
  try {
    sessions.value = await chatApi.sessions()
    if (sessions.value[0]) {
      await selectSession(sessions.value[0].id)
    }
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    loading.value = false
  }
}

async function selectSession(sessionId: string) {
  activeSessionId.value = sessionId
  messages.value = await chatApi.messages(sessionId)
}

async function createSession() {
  errorMessage.value = ""
  try {
    const session = await chatApi.createSession()
    sessions.value.unshift(session)
    await selectSession(session.id)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  }
}

function applySuggestion(question: string) {
  chatInput.value = question
}

async function sendQuestion() {
  if (!chatInput.value.trim()) return
  if (!activeSessionId.value) await createSession()
  if (!activeSessionId.value) return

  const question = chatInput.value.trim()
  chatInput.value = ""
  errorMessage.value = ""
  streaming.value = true

  const assistantMessage: ChatMessage = {
    id: crypto.randomUUID(),
    role: "assistant",
    content: "",
    references: [],
  }

  messages.value.push({ id: crypto.randomUUID(), role: "user", content: question })
  messages.value.push(assistantMessage)

  try {
    await chatApi.stream(
      { sessionId: activeSessionId.value, question, maxReferences: 5 },
      ({ event, data }) => {
        if (event === "reference" && Array.isArray(data)) {
          assistantMessage.references = data as ChatReference[]
        }
        if (event === "delta" && typeof data === "object" && data && "content" in data) {
          assistantMessage.content += String((data as { content: unknown }).content)
        }
        if (event === "done" && typeof data === "object" && data && "messageId" in data) {
          assistantMessage.id = String((data as { messageId: unknown }).messageId)
        }
      },
    )
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  } finally {
    streaming.value = false
  }
}

async function saveAnswer(targetType: "synthesis" | "question") {
  if (!activeSessionId.value || !lastAssistantMessage.value) return
  try {
    await chatApi.saveAnswer({
      sessionId: activeSessionId.value,
      messageId: lastAssistantMessage.value.id,
      targetType,
    })
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  }
}

function handleSaveSelect(key: string | number) {
  if (key === "synthesis" || key === "question") void saveAnswer(key)
}

async function openReference(ref: ChatReference) {
  try {
    await wikiApi.open(ref.path)
  } catch (error) {
    errorMessage.value = toErrorMessage(error)
  }
}

onMounted(loadSessions)
</script>

<template>
  <section class="page-grid chat-grid">
    <NAlert v-if="errorMessage" type="error" :bordered="false">
      {{ errorMessage }}
    </NAlert>

    <aside class="section-panel conversation-list">
      <NButton type="primary" block @click="createSession">
        <template #icon>
          <AppIcon name="plus" />
        </template>
        新建对话
      </NButton>
      <NSpin v-if="loading" />
      <NEmpty v-else-if="sessions.length === 0" description="暂无对话" />
      <template v-else>
        <button
          v-for="session in sessions"
          :key="session.id"
          class="conversation-item"
          :class="{ active: activeSessionId === session.id }"
          type="button"
          @click="selectSession(session.id)"
        >
          <strong>{{ session.title }}</strong>
          <span>{{ session.updatedAt }}</span>
        </button>
      </template>
    </aside>

    <main class="section-panel chat-panel">
      <div class="message-stream">
        <NEmpty v-if="messages.length === 0" description="选择一个会话或新建对话后开始提问" />
        <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="message.role === 'user' ? 'user-message' : 'assistant-message'"
        >
          <AppIcon v-if="message.role === 'assistant'" name="spark" />
          <div style="flex: 1">
            <span v-if="message.pinned" class="message-pin">
              <AppIcon name="save" :size="11" />
              已固定
            </span>
            <p>{{ message.content }}</p>
            <NSpace v-if="message.references" :size="6">
              <NTag
                v-for="ref in message.references"
                :key="ref.id"
                round
                type="info"
                size="small"
                @click="openReference(ref)"
              >
                {{ `[${ref.id}] ${ref.title}` }}
              </NTag>
            </NSpace>
          </div>
        </div>
      </div>

      <div class="composer">
        <div class="suggested-questions">
          <strong>主动推荐</strong>
          <button
            v-for="(suggestion, idx) in chatSuggestions"
            :key="idx"
            class="suggestion-chip"
            type="button"
            @click="applySuggestion(suggestion.question)"
          >
            <span>{{ suggestion.question }}</span>
            <small>{{ suggestion.reason }}</small>
          </button>
        </div>

        <NInput
          v-model:value="chatInput"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 6 }"
          placeholder="基于当前 Vault 提问，回答会自动检索 wiki/ 中的相关页面"
        />
        <NSpace justify="space-between" :size="8">
          <NButton tertiary>
            <template #icon>
              <AppIcon name="save" />
            </template>
            固定上下文
          </NButton>
          <NSpace :size="8">
            <NDropdown
              :options="saveOptions"
              trigger="click"
              @select="handleSaveSelect"
            >
              <NButton secondary>
                <template #icon>
                  <AppIcon name="save" />
                </template>
                保存到 Wiki
              </NButton>
            </NDropdown>
            <NButton type="primary" :loading="streaming" @click="sendQuestion">
              <template #icon>
                <AppIcon name="spark" />
              </template>
              发送
            </NButton>
          </NSpace>
        </NSpace>
      </div>
    </main>

    <aside class="section-panel reference-panel">
      <div class="section-toolbar">
        <div>
          <h2>引用来源</h2>
          <p>点击在 Wiki 预览中打开对应 Markdown。</p>
        </div>
      </div>
      <button
        v-for="ref in references"
        :key="ref.id"
        class="reference-item"
        type="button"
        @click="openReference(ref)"
      >
        <strong>{{ `[${ref.id}] ${ref.title}` }}</strong>
        <span>{{ ref.path }}</span>
        <small>{{ ref.quote }}</small>
      </button>
    </aside>
  </section>
</template>
