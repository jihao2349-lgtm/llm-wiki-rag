package com.jihao.aiwiki.domain.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM chat message.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage {

    /** Message role */
    private String role;

    /** Message content */
    private String content;
}
