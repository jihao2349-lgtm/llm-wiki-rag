package com.jihao.aiwiki.domain.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLM stream parser tests.
 *
 * @author jihao
 * @date 2026/05/06
 */
class LlmStreamParserTest {

    /** Parser under test */
    private final LlmStreamParser parser = new LlmStreamParser(new ObjectMapper());

    /**
     * 验证 OpenAI-compatible delta content 可被解析。
     */
    @Test
    void parseLineShouldReturnDeltaContent() {
        Optional<LlmStreamDelta> delta = parser.parseLine("""
                data: {"choices":[{"delta":{"content":"你好"},"finish_reason":null}]}
                """.trim());

        assertThat(delta).isPresent();
        assertThat(delta.get().getContent()).isEqualTo("你好");
        assertThat(delta.get().isDone()).isFalse();
    }

    /**
     * 验证 [DONE] 可被解析为结束事件。
     */
    @Test
    void parseLineShouldReturnDoneEvent() {
        Optional<LlmStreamDelta> delta = parser.parseLine("data: [DONE]");

        assertThat(delta).isPresent();
        assertThat(delta.get().isDone()).isTrue();
    }

    /**
     * 验证无 delta 的 SSE 行被忽略。
     */
    @Test
    void parseLineShouldIgnoreBlankOrInvalidLine() {
        assertThat(parser.parseLine("event: ping")).isEmpty();
        assertThat(parser.parseLine("data: {}")).isEmpty();
    }
}
