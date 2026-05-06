package com.jihao.aiwiki.domain.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * OpenAI-compatible stream delta parser.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class LlmStreamParser {

    /** SSE data prefix */
    private static final String DATA_PREFIX = "data:";

    /** Stream done marker */
    private static final String DONE_MARKER = "[DONE]";

    /** JSON mapper */
    private final ObjectMapper objectMapper;

    /**
     * 创建 parser。
     *
     * @param objectMapper JSON mapper
     */
    public LlmStreamParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析单行 SSE delta。
     *
     * @param line SSE line
     * @return delta event
     */
    public Optional<LlmStreamDelta> parseLine(String line) {
        if (!StringUtils.hasText(line) || !line.startsWith(DATA_PREFIX)) {
            return Optional.empty();
        }
        String data = line.substring(DATA_PREFIX.length()).trim();
        if (!StringUtils.hasText(data)) {
            return Optional.empty();
        }
        if (DONE_MARKER.equals(data)) {
            return Optional.of(new LlmStreamDelta("", true));
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choice = root.path("choices").path(0);
            String finishReason = choice.path("finish_reason").asText(null);
            JsonNode contentNode = choice.path("delta").path("content");
            String content = contentNode.isMissingNode() || contentNode.isNull() ? "" : contentNode.asText();
            boolean done = StringUtils.hasText(finishReason);
            if (!StringUtils.hasText(content) && !done) {
                return Optional.empty();
            }
            return Optional.of(new LlmStreamDelta(content, done));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}
