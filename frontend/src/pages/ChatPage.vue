<script setup lang="ts">
import { ref } from "vue"
import { NButton, NDropdown, NInput, NSpace, NTag } from "naive-ui"
import AppIcon from "../components/AppIcon.vue"
import { chatMessages, chatSuggestions, references } from "../mock-data"

const chatInput = ref("这些资料里关于 Agent Memory 的观点有哪些？")

const saveOptions = [
  { key: "synthesis", label: "保存为合成观点 (synthesis)" },
  { key: "question", label: "保存为开放问题 (question)" },
  { key: "append", label: "追加到已有页面" },
]

function applySuggestion(question: string) {
  chatInput.value = question
}
</script>

<template>
  <section class="page-grid chat-grid">
    <aside class="section-panel conversation-list">
      <NButton type="primary" block>
        <template #icon>
          <AppIcon name="plus" />
        </template>
        新建对话
      </NButton>
      <button class="conversation-item active" type="button">
        <strong>Agent Memory 观点汇总</strong>
        <span>3 条引用 · 刚刚</span>
      </button>
      <button class="conversation-item" type="button">
        <strong>本地优先知识库设计</strong>
        <span>5 条引用 · 昨天</span>
      </button>
      <button class="conversation-item" type="button">
        <strong>RAG 与长上下文比较</strong>
        <span>2 条引用 · 周二</span>
      </button>
      <button class="conversation-item" type="button">
        <strong>多模态摄入流程</strong>
        <span>4 条引用 · 上周</span>
      </button>
    </aside>

    <main class="section-panel chat-panel">
      <div class="message-stream">
        <div
          v-for="message in chatMessages"
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
            >
              <NButton secondary>
                <template #icon>
                  <AppIcon name="save" />
                </template>
                保存到 Wiki
              </NButton>
            </NDropdown>
            <NButton type="primary">
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
      <button v-for="ref in references" :key="ref.id" class="reference-item" type="button">
        <strong>{{ `[${ref.id}] ${ref.title}` }}</strong>
        <span>{{ ref.path }}</span>
        <small>{{ ref.quote }}</small>
      </button>
    </aside>
  </section>
</template>
