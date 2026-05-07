package com.jihao.aiwiki.domain.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI-compatible HTTP LLM client.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    /** Chat completions path */
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /** HTTP request timeout — ingest prompts can be large, allow 5 minutes */
    private static final Duration TIMEOUT = Duration.ofSeconds(300);

    /** HTTP client */
    private final HttpClient httpClient;

    /** JSON mapper */
    private final ObjectMapper objectMapper;

    /** Stream parser */
    private final LlmStreamParser streamParser;

    /**
     * 创建 LLM client。
     *
     * @param objectMapper JSON mapper
     * @param streamParser stream parser
     */
    public OpenAiCompatibleLlmClient(ObjectMapper objectMapper, LlmStreamParser streamParser) {
        this.objectMapper = objectMapper;
        this.streamParser = streamParser;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        try {
            HttpRequest httpRequest = buildRequest(request, false);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm call failed with status " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            String model = root.path("model").asText(request.getModel());
            return new LlmChatResponse(model, content);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED,
                    "llm call failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    @Override
    public void streamChat(LlmChatRequest request, Consumer<LlmStreamDelta> deltaConsumer) {
        try {
            HttpRequest httpRequest = buildRequest(request, true);
            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm stream failed with status " + response.statusCode());
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    streamParser.parseLine(line).ifPresent(deltaConsumer);
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED,
                    "llm stream failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    /**
     * 创建 OpenAI-compatible HTTP request。
     *
     * @param request LLM 请求
     * @param stream 是否流式
     * @return HTTP request
     */
    private HttpRequest buildRequest(LlmChatRequest request, boolean stream) {
        validateRequest(request);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", request.getModel());
            payload.put("messages", request.getMessages());
            payload.put("temperature", request.getTemperature() == null ? BigDecimal.valueOf(0.2) : request.getTemperature());
            payload.put("stream", stream);
            String body = objectMapper.writeValueAsString(payload);
            return HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(request.getBaseUrl()) + CHAT_COMPLETIONS_PATH))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + request.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "build llm request failed");
        }
    }

    /**
     * 校验请求必要字段。
     *
     * @param request LLM 请求
     */
    private void validateRequest(LlmChatRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getBaseUrl())
                || !StringUtils.hasText(request.getApiKey())
                || !StringUtils.hasText(request.getModel())) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm settings incomplete");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "llm messages empty");
        }
    }

    /**
     * 规范化 base URL。
     *
     * @param baseUrl 原始 base URL
     * @return 规范化 URL
     */
    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/chat/completions")) {
            return normalized.substring(0, normalized.length() - CHAT_COMPLETIONS_PATH.length());
        }
        return normalized;
    }
}
