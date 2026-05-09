package com.jihao.aiwiki.domain.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Embedding HTTP 客户端。
 * POST {baseUrl}/embeddings，失败指数退避重试最多 2 次。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Component
public class EmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/embeddings";
    private static final int MAX_RETRIES = 2;
    private static final int MAX_TEXT_CHARS = 2000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 批量向量化，texts.size() 须 <= config.batchSize。
     *
     * @param texts  待向量化文本列表
     * @param config Embedding 配置
     * @return 向量数组列表，与 texts 一一对应
     */
    public List<float[]> embed(List<String> texts, EmbeddingConfig config) {
        List<String> truncated = texts.stream()
                .map(t -> t.length() > MAX_TEXT_CHARS ? t.substring(0, MAX_TEXT_CHARS) : t)
                .toList();
        String body = buildRequestBody(truncated, config);
        String responseJson = callWithRetry(config, body);
        return parseEmbeddings(responseJson, texts.size());
    }

    /**
     * 单文本向量化（query 场景）。
     */
    public float[] embedSingle(String text, EmbeddingConfig config) {
        return embed(List.of(text), config).get(0);
    }

    private String buildRequestBody(List<String> texts, EmbeddingConfig config) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", config.getModel());
            payload.put("input", texts);
            payload.put("encoding_format", "float");
            payload.put("dimensions", config.getDimension());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new EmbeddingException("failed to build embedding request body", e);
        }
    }

    private String callWithRetry(EmbeddingConfig config, String body) {
        String url = normalizeBaseUrl(config.getBaseUrl()) + EMBEDDINGS_PATH;
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                sleep(Duration.ofMillis(500L * (1L << (attempt - 1))));
            }
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                }
                EmbeddingException ex = new EmbeddingException(
                        "embedding API returned status " + status + ": " + truncate(response.body(), 200));
                // 4xx = client-side config error, no point retrying
                if (status >= 400 && status < 500) {
                    throw ex;
                }
                lastException = ex;
            } catch (EmbeddingException e) {
                throw e;
            } catch (Exception e) {
                lastException = new EmbeddingException("embedding API call failed: " + e.getMessage(), e);
            }
        }
        throw (EmbeddingException) lastException;
    }

    private List<float[]> parseEmbeddings(String json, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() < expectedCount) {
                throw new EmbeddingException("unexpected embedding response: data size=" + data.size() + ", expected=" + expectedCount);
            }
            List<float[]> result = new ArrayList<>(expectedCount);
            for (int i = 0; i < expectedCount; i++) {
                JsonNode embNode = data.get(i).path("embedding");
                if (!embNode.isArray()) {
                    throw new EmbeddingException("embedding field is not an array at index " + i);
                }
                float[] vec = new float[embNode.size()];
                for (int j = 0; j < vec.length; j++) {
                    vec[j] = (float) embNode.get(j).asDouble();
                }
                result.add(vec);
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("failed to parse embedding response", e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
