<script setup lang="ts">
import { computed, inject, nextTick, onMounted, onUnmounted, ref } from "vue"
import { NAlert, NButton, NDropdown, NEmpty, NInput, NSpace, NSpin, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { chatApi, generateUUID } from "../api/client"
import { toErrorMessage } from "../utils/api-state"
import type { ChatMessage, ChatReference, ChatSession, PageKey } from "../types"

const navigateTo = inject<(page: PageKey) => void>('navigateTo')

// ---- Resizable reference panel ----
const REF_MIN = 200
const REF_MAX = 600
const refPanelWidth = ref(320)
const isDragging = ref(false)

function onDividerMousedown(e: MouseEvent) {
  e.preventDefault()
  isDragging.value = true
  const startX = e.clientX
  const startWidth = refPanelWidth.value

  function onMousemove(ev: MouseEvent) {
    // dragging left = wider ref panel
    refPanelWidth.value = Math.min(REF_MAX, Math.max(REF_MIN, startWidth - (ev.clientX - startX)))
  }
  function onMouseup() {
    isDragging.value = false
    window.removeEventListener('mousemove', onMousemove)
    window.removeEventListener('mouseup', onMouseup)
  }
  window.addEventListener('mousemove', onMousemove)
  window.addEventListener('mouseup', onMouseup)
}

onUnmounted(() => { isDragging.value = false })

const messageStreamRef = ref<HTMLElement>()

function scrollToBottom() {
  nextTick(() => {
    if (messageStreamRef.value) {
      messageStreamRef.value.scrollTop = messageStreamRef.value.scrollHeight
    }
  })
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    void sendQuestion()
  }
}

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

  messages.value.push({ id: generateUUID(), role: "user", content: question })
  messages.value.push({ id: generateUUID(), role: "assistant", content: "", references: [] })
  // Get the reactive proxy reference AFTER push so mutations trigger Vue updates
  const assistantIdx = messages.value.length - 1
  scrollToBottom()

  try {
    await chatApi.stream(
      { sessionId: activeSessionId.value, question, maxReferences: 5 },
      ({ event, data }) => {
        const msg = messages.value[assistantIdx]
        if (event === "reference" && Array.isArray(data)) {
          msg.references = data as ChatReference[]
        }
        if (event === "delta" && typeof data === "object" && data && "content" in data) {
          msg.content += String((data as { content: unknown }).content)
          scrollToBottom()
        }
        if (event === "done" && typeof data === "object" && data && "messageId" in data) {
          msg.id = String((data as { messageId: unknown }).messageId)
          scrollToBottom()
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

function openReference(_ref: ChatReference) {
  navigateTo?.('wiki')
}

onMounted(loadSessions)
</script>

<template>
  <section
    class="page-grid chat-grid"
    :style="{ gridTemplateColumns: `260px minmax(0, 1fr) 4px ${refPanelWidth}px` }"
    :class="{ 'chat-grid--dragging': isDragging }"
  >
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
      <div ref="messageStreamRef" class="message-stream">
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
        <div v-if="messages.length === 0" class="suggested-questions">
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
          :autosize="{ minRows: 2, maxRows: 5 }"
          :disabled="streaming"
          placeholder="提问… Enter 发送，Shift+Enter 换行"
          @keydown="handleKeydown"
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

    <div class="wiki-divider" @mousedown="onDividerMousedown">
      <div class="wiki-divider__handle" />
    </div>

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
