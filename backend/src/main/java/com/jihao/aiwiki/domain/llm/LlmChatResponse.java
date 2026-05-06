package com.jihao.aiwiki.domain.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM chat completion response.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse {

    /** 模型名称 */
    private String model;

    /** Assistant content */
    private String content;
}
