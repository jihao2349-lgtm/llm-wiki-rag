package com.jihao.aiwiki.domain.llm;

import java.util.function.Consumer;

/**
 * 统一 LLM 调用接口。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface LlmClient {

    /**
     * 发起非流式 chat completion。
     *
     * @param request chat 请求
     * @return chat 响应
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 发起流式 chat completion。
     *
     * @param request chat 请求
     * @param deltaConsumer delta 回调
     */
    void streamChat(LlmChatRequest request, Consumer<LlmStreamDelta> deltaConsumer);
}
