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
 * OpenAI 兼容 Embedding HTTP 客户端，兼容火山方舟 doubao-embedding-vision 多模态接口。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Component
public class EmbeddingClient {

    private static final String EMBEDDINGS_PATH = "/embeddings";
    // 火山方舟 doubao-embedding-vision 专用路径
    private static final String MULTIMODAL_EMBEDDINGS_PATH = "/embeddings/multimodal";
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
     * 批量向量化。vision 模型不支持批量，逐条调用后合并。
     */
    public List<float[]> embed(List<String> texts, EmbeddingConfig config) {
        if (config.isVisionInputFormat()) {
            List<float[]> result = new ArrayList<>(texts.size());
            for (String text : texts) {
                result.add(embedSingleVision(text, config));
            }
            return result;
        }
        List<String> truncated = texts.stream()
                .map(t -> t.length() > MAX_TEXT_CHARS ? t.substring(0, MAX_TEXT_CHARS) : t)
                .toList();
        String body = buildTextRequestBody(truncated, config);
        String responseJson = callWithRetry(config, body);
        return parseTextEmbeddings(responseJson, texts.size());
    }

    /**
     * 单文本向量化（query 场景）。
     */
    public float[] embedSingle(String text, EmbeddingConfig config) {
        return embed(List.of(text), config).get(0);
    }

    // ---- request builders ----

    private String buildTextRequestBody(List<String> texts, EmbeddingConfig config) {
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

    private float[] embedSingleVision(String text, EmbeddingConfig config) {
        String truncated = text.length() > MAX_TEXT_CHARS ? text.substring(0, MAX_TEXT_CHARS) : text;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", config.getModel());
            payload.put("input", List.of(Map.of("type", "text", "text", truncated)));
            String body = objectMapper.writeValueAsString(payload);
            String responseJson = callWithRetry(config, body);
            return parseVisionEmbedding(responseJson);
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("failed to build vision embedding request", e);
        }
    }

    // ---- HTTP ----

    private String callWithRetry(EmbeddingConfig config, String body) {
        String path = config.isVisionInputFormat() ? MULTIMODAL_EMBEDDINGS_PATH : EMBEDDINGS_PATH;
        String url = normalizeBaseUrl(config.getBaseUrl()) + path;
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                sleep(Duration.ofMillis(500L * (1L << (attempt - 1))));
            }
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + config.getApiKey());
                HttpRequest request = builder
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                }
                EmbeddingException ex = new EmbeddingException(
                        "embedding API returned status " + status + ": " + truncate(response.body(), 200));
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

    // ---- response parsers ----

    /** 标准 OpenAI 格式：data[i].embedding */
    private List<float[]> parseTextEmbeddings(String json, int expectedCount) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.size() < expectedCount) {
                throw new EmbeddingException("unexpected embedding response: data size=" + data.size() + ", expected=" + expectedCount);
            }
            List<float[]> result = new ArrayList<>(expectedCount);
            for (int i = 0; i < expectedCount; i++) {
                result.add(extractVector(data.get(i).path("embedding"), i));
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("failed to parse embedding response", e);
        }
    }

    /** 火山方舟 multimodal 格式：data.embedding（对象，非数组） */
    private float[] parseVisionEmbedding(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode embNode = root.path("data").path("embedding");
            return extractVector(embNode, 0);
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("failed to parse vision embedding response", e);
        }
    }

    private float[] extractVector(JsonNode embNode, int index) {
        if (!embNode.isArray()) {
            throw new EmbeddingException("embedding field is not an array at index " + index);
        }
        float[] vec = new float[embNode.size()];
        for (int j = 0; j < vec.length; j++) {
            vec[j] = (float) embNode.get(j).asDouble();
        }
        return vec;
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
