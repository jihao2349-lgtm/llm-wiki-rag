package com.jihao.aiwiki.domain.llm;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * LLM chat completion request.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
public class LlmChatRequest {

    /** Provider 标识 */
    private String provider;

    /** API Base URL */
    private String baseUrl;

    /** API Key 明文，仅内存中使用 */
    private String apiKey;

    /** 模型名称 */
    private String model;

    /** 消息列表 */
    private List<LlmMessage> messages;

    /** 采样温度 */
    private BigDecimal temperature;

    /** 是否使用流式返回 */
    private boolean stream;
}
