package com.jihao.aiwiki.domain.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM stream delta event.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmStreamDelta {

    /** Delta content */
    private String content;

    /** 是否为结束事件 */
    private boolean done;
}
