package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.domain.embedding.EmbeddingConfig;
import com.jihao.aiwiki.domain.llm.LlmChatRequest;
import com.jihao.aiwiki.domain.llm.LlmChatResponse;
import com.jihao.aiwiki.domain.llm.LlmClient;
import com.jihao.aiwiki.domain.llm.LlmMessage;
import com.jihao.aiwiki.domain.llm.SensitiveLogSanitizer;
import com.jihao.aiwiki.domain.vault.SecretCipher;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.dto.setting.SettingTestRequest;
import com.jihao.aiwiki.dto.setting.SettingUpdateRequest;
import com.jihao.aiwiki.entity.AppSettingDO;
import com.jihao.aiwiki.mapper.AppSettingMapper;
import com.jihao.aiwiki.service.SettingService;
import com.jihao.aiwiki.vo.setting.SettingDetailVO;
import com.jihao.aiwiki.vo.setting.SettingTestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 应用配置服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private static final String KEY_PROVIDER = "llm.provider";
    private static final String KEY_BASE_URL = "llm.base_url";
    private static final String KEY_API_KEY_CIPHER = "llm.api_key_cipher";
    private static final String KEY_MODEL = "llm.model";
    private static final String KEY_MAX_CONTEXT_SIZE = "llm.max_context_size";
    private static final String KEY_TEMPERATURE = "llm.temperature";
    private static final String KEY_OUTPUT_LANGUAGE = "llm.output_language";
    private static final String KEY_EMBEDDING_ENABLED = "llm.embedding_enabled";
    private static final String KEY_EMBEDDING_BASE_URL = "llm.embedding_base_url";
    private static final String KEY_EMBEDDING_API_KEY_CIPHER = "llm.embedding_api_key_cipher";
    private static final String KEY_EMBEDDING_MODEL = "llm.embedding_model";
    private static final String KEY_EMBEDDING_DIMENSION = "llm.embedding_dimension";
    private static final String KEY_EMBEDDING_BATCH_SIZE = "llm.embedding_batch_size";

    private static final List<String> SETTING_KEYS;
    static {
        List<String> keys = new ArrayList<>(List.of(
                KEY_PROVIDER, KEY_BASE_URL, KEY_API_KEY_CIPHER, KEY_MODEL,
                KEY_MAX_CONTEXT_SIZE, KEY_TEMPERATURE, KEY_OUTPUT_LANGUAGE,
                KEY_EMBEDDING_ENABLED, KEY_EMBEDDING_BASE_URL, KEY_EMBEDDING_API_KEY_CIPHER,
                KEY_EMBEDDING_MODEL, KEY_EMBEDDING_DIMENSION, KEY_EMBEDDING_BATCH_SIZE
        ));
        SETTING_KEYS = List.copyOf(keys);
    }

    private static final String DEFAULT_PROVIDER = "openai-compatible";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final int DEFAULT_MAX_CONTEXT_SIZE = 32000;
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.2);
    private static final String DEFAULT_OUTPUT_LANGUAGE = "Chinese";

    private final Optional<AppSettingMapper> appSettingMapper;
    private final SecretCipher secretCipher;
    private final LlmClient llmClient;

    @Override
    public SettingDetailVO getDetail() {
        Map<String, String> settings = loadSettings();
        return toDetail(settings);
    }

    @Override
    public SettingDetailVO update(SettingUpdateRequest request) {
        Map<String, String> current = loadSettings();
        upsert(KEY_PROVIDER, request.getProvider().trim(), "STRING", "LLM provider");
        upsert(KEY_BASE_URL, request.getBaseUrl().trim(), "STRING", "OpenAI-compatible API base URL");
        upsert(KEY_MODEL, request.getModel().trim(), "STRING", "Chat completion model");
        upsert(KEY_MAX_CONTEXT_SIZE, String.valueOf(valueOrDefault(request.getMaxContextSize(), DEFAULT_MAX_CONTEXT_SIZE)),
                "INTEGER", "Max context size");
        upsert(KEY_TEMPERATURE, valueOrDefault(request.getTemperature(), DEFAULT_TEMPERATURE).toPlainString(),
                "DECIMAL", "LLM temperature");
        upsert(KEY_OUTPUT_LANGUAGE, request.getOutputLanguage().trim(), "STRING", "Output language");
        upsert(KEY_EMBEDDING_ENABLED, String.valueOf(Boolean.TRUE.equals(request.getEmbeddingEnabled())),
                "BOOLEAN", "Embedding enabled");

        if (StringUtils.hasText(request.getApiKey()) && !secretCipher.isMasked(request.getApiKey())) {
            upsert(KEY_API_KEY_CIPHER, secretCipher.encrypt(request.getApiKey().trim()), "SECRET", "Encrypted LLM API key");
        } else if (StringUtils.hasText(current.get(KEY_API_KEY_CIPHER))) {
            upsert(KEY_API_KEY_CIPHER, current.get(KEY_API_KEY_CIPHER), "SECRET", "Encrypted LLM API key");
        }

        if (StringUtils.hasText(request.getEmbeddingBaseUrl())) {
            upsert(KEY_EMBEDDING_BASE_URL, request.getEmbeddingBaseUrl().trim(), "STRING", "Embedding API base URL");
        }
        if (StringUtils.hasText(request.getEmbeddingModel())) {
            upsert(KEY_EMBEDDING_MODEL, request.getEmbeddingModel().trim(), "STRING", "Embedding model");
        }
        if (request.getEmbeddingDimension() != null) {
            upsert(KEY_EMBEDDING_DIMENSION, String.valueOf(request.getEmbeddingDimension()), "INTEGER", "Embedding dimension");
        }
        if (request.getEmbeddingBatchSize() != null) {
            upsert(KEY_EMBEDDING_BATCH_SIZE, String.valueOf(request.getEmbeddingBatchSize()), "INTEGER", "Embedding batch size");
        }
        if (StringUtils.hasText(request.getEmbeddingApiKey()) && !secretCipher.isMasked(request.getEmbeddingApiKey())) {
            upsert(KEY_EMBEDDING_API_KEY_CIPHER, secretCipher.encrypt(request.getEmbeddingApiKey().trim()),
                    "SECRET", "Encrypted embedding API key");
        } else if (StringUtils.hasText(current.get(KEY_EMBEDDING_API_KEY_CIPHER))) {
            upsert(KEY_EMBEDDING_API_KEY_CIPHER, current.get(KEY_EMBEDDING_API_KEY_CIPHER),
                    "SECRET", "Encrypted embedding API key");
        }

        return getDetail();
    }

    @Override
    public SettingTestVO testLlm(SettingTestRequest request) {
        Map<String, String> settings = loadSettings();
        String apiKey = resolveApiKey(request, settings);
        String model = firstText(request.getModel(), settings.get(KEY_MODEL), DEFAULT_MODEL);
        if (!StringUtils.hasText(apiKey)) {
            return SettingTestVO.builder()
                    .success(false)
                    .message("LLM API Key is not configured")
                    .model(model)
                    .build();
        }
        LlmChatRequest chatRequest = LlmChatRequest.builder()
                .provider(firstText(request.getProvider(), settings.get(KEY_PROVIDER), DEFAULT_PROVIDER))
                .baseUrl(firstText(request.getBaseUrl(), settings.get(KEY_BASE_URL), DEFAULT_BASE_URL))
                .apiKey(apiKey)
                .model(model)
                .temperature(valueOrDefault(request.getTemperature(), parseDecimal(settings.get(KEY_TEMPERATURE), DEFAULT_TEMPERATURE)))
                .messages(List.of(
                        new LlmMessage("system", "You are a connectivity test endpoint. Reply with OK."),
                        new LlmMessage("user", "ping")
                ))
                .stream(false)
                .build();
        try {
            LlmChatResponse response = llmClient.chat(chatRequest);
            return SettingTestVO.builder()
                    .success(true)
                    .message("LLM connectivity OK")
                    .model(firstText(response.getModel(), model))
                    .build();
        } catch (RuntimeException exception) {
            return SettingTestVO.builder()
                    .success(false)
                    .message(SensitiveLogSanitizer.sanitize(exception.getMessage()))
                    .model(model)
                    .build();
        }
    }

    @Override
    public EmbeddingConfig getEmbeddingConfig() {
        Map<String, String> settings = loadSettings();
        boolean enabled = Boolean.parseBoolean(settings.getOrDefault(KEY_EMBEDDING_ENABLED, "false"));
        String embCipher = settings.get(KEY_EMBEDDING_API_KEY_CIPHER);
        String embApiKey = StringUtils.hasText(embCipher) ? secretCipher.decrypt(embCipher) : null;
        String embBaseUrl = firstText(settings.get(KEY_EMBEDDING_BASE_URL), EmbeddingConfig.DEFAULT_BASE_URL);
        String model = firstText(settings.get(KEY_EMBEDDING_MODEL), EmbeddingConfig.DEFAULT_MODEL);
        // 火山方舟 URL 判断是否用 vision 多模态接口（接入点 ID 不含 "vision"，用 URL 判断更可靠）
        boolean visionFormat = (embBaseUrl != null && embBaseUrl.contains("volces.com"))
                || (model != null && model.contains("vision"));
        return EmbeddingConfig.builder()
                .enabled(enabled)
                .baseUrl(embBaseUrl)
                .apiKey(embApiKey != null ? embApiKey : "")
                .model(model)
                .dimension(parseInteger(settings.get(KEY_EMBEDDING_DIMENSION), EmbeddingConfig.DEFAULT_DIMENSION))
                .batchSize(parseInteger(settings.get(KEY_EMBEDDING_BATCH_SIZE), EmbeddingConfig.DEFAULT_BATCH_SIZE))
                .visionInputFormat(visionFormat)
                .build();
    }

    private Map<String, String> loadSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        for (AppSettingDO setting : mapper().findByKeys(SETTING_KEYS)) {
            settings.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return settings;
    }

    private void upsert(String key, String value, String valueType, String description) {
        AppSettingDO existing = mapper().findByKey(key);
        if (existing == null) {
            AppSettingDO setting = new AppSettingDO();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            setting.setValueType(valueType);
            setting.setDescription(description);
            mapper().insert(setting);
        } else {
            mapper().updateValue(key, value);
        }
    }

    private AppSettingMapper mapper() {
        return appSettingMapper.orElseThrow(() ->
                new BusinessException(ErrorCode.LLM_CALL_FAILED, "app setting mapper is not available"));
    }

    private SettingDetailVO toDetail(Map<String, String> settings) {
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        String plainKey = StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
        String embCipher = settings.get(KEY_EMBEDDING_API_KEY_CIPHER);
        String embPlainKey = StringUtils.hasText(embCipher) ? secretCipher.decrypt(embCipher) : null;
        return SettingDetailVO.builder()
                .provider(firstText(settings.get(KEY_PROVIDER), DEFAULT_PROVIDER))
                .baseUrl(firstText(settings.get(KEY_BASE_URL), DEFAULT_BASE_URL))
                .apiKeyMasked(secretCipher.mask(plainKey))
                .model(firstText(settings.get(KEY_MODEL), DEFAULT_MODEL))
                .maxContextSize(parseInteger(settings.get(KEY_MAX_CONTEXT_SIZE), DEFAULT_MAX_CONTEXT_SIZE))
                .temperature(parseDecimal(settings.get(KEY_TEMPERATURE), DEFAULT_TEMPERATURE))
                .outputLanguage(firstText(settings.get(KEY_OUTPUT_LANGUAGE), DEFAULT_OUTPUT_LANGUAGE))
                .embeddingEnabled(Boolean.parseBoolean(settings.getOrDefault(KEY_EMBEDDING_ENABLED, "false")))
                .embeddingBaseUrl(firstText(settings.get(KEY_EMBEDDING_BASE_URL), EmbeddingConfig.DEFAULT_BASE_URL))
                .embeddingApiKeyMasked(secretCipher.mask(embPlainKey))
                .embeddingModel(firstText(settings.get(KEY_EMBEDDING_MODEL), EmbeddingConfig.DEFAULT_MODEL))
                .embeddingDimension(parseInteger(settings.get(KEY_EMBEDDING_DIMENSION), EmbeddingConfig.DEFAULT_DIMENSION))
                .embeddingBatchSize(parseInteger(settings.get(KEY_EMBEDDING_BATCH_SIZE), EmbeddingConfig.DEFAULT_BATCH_SIZE))
                .configured(StringUtils.hasText(plainKey))
                .build();
    }

    private String resolveApiKey(SettingTestRequest request, Map<String, String> settings) {
        if (StringUtils.hasText(request.getApiKey()) && !secretCipher.isMasked(request.getApiKey())) {
            return request.getApiKey().trim();
        }
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        return StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private BigDecimal parseDecimal(String value, BigDecimal defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
