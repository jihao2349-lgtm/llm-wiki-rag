package com.jihao.aiwiki.service.impl;

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

    /** Provider 配置键 */
    private static final String KEY_PROVIDER = "llm.provider";

    /** Base URL 配置键 */
    private static final String KEY_BASE_URL = "llm.base_url";

    /** API Key 密文配置键 */
    private static final String KEY_API_KEY_CIPHER = "llm.api_key_cipher";

    /** Model 配置键 */
    private static final String KEY_MODEL = "llm.model";

    /** 最大上下文配置键 */
    private static final String KEY_MAX_CONTEXT_SIZE = "llm.max_context_size";

    /** 温度配置键 */
    private static final String KEY_TEMPERATURE = "llm.temperature";

    /** 输出语言配置键 */
    private static final String KEY_OUTPUT_LANGUAGE = "llm.output_language";

    /** Embedding 开关配置键 */
    private static final String KEY_EMBEDDING_ENABLED = "llm.embedding_enabled";

    /** T6 管理的配置键 */
    private static final List<String> SETTING_KEYS = List.of(
            KEY_PROVIDER,
            KEY_BASE_URL,
            KEY_API_KEY_CIPHER,
            KEY_MODEL,
            KEY_MAX_CONTEXT_SIZE,
            KEY_TEMPERATURE,
            KEY_OUTPUT_LANGUAGE,
            KEY_EMBEDDING_ENABLED
    );

    /** 默认 provider */
    private static final String DEFAULT_PROVIDER = "openai-compatible";

    /** 默认 OpenAI-compatible base URL */
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    /** 默认模型 */
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";

    /** 默认最大上下文 */
    private static final int DEFAULT_MAX_CONTEXT_SIZE = 32000;

    /** 默认温度 */
    private static final BigDecimal DEFAULT_TEMPERATURE = BigDecimal.valueOf(0.2);

    /** 默认输出语言 */
    private static final String DEFAULT_OUTPUT_LANGUAGE = "Chinese";

    /** 配置 mapper */
    private final Optional<AppSettingMapper> appSettingMapper;

    /** 密钥加密工具 */
    private final SecretCipher secretCipher;

    /** 统一 LLM client */
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

    /**
     * 加载 T6 管理的所有配置。
     *
     * @return key-value 配置
     */
    private Map<String, String> loadSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        for (AppSettingDO setting : mapper().findByKeys(SETTING_KEYS)) {
            settings.put(setting.getSettingKey(), setting.getSettingValue());
        }
        return settings;
    }

    /**
     * 写入或更新配置项。
     *
     * @param key 配置键
     * @param value 配置值
     * @param valueType 值类型
     * @param description 配置说明
     */
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

    /**
     * 获取 mapper；无 DataSource 的 smoke test 中允许应用上下文加载，但业务调用会明确失败。
     *
     * @return app_setting mapper
     */
    private AppSettingMapper mapper() {
        return appSettingMapper.orElseThrow(() ->
                new BusinessException(ErrorCode.LLM_CALL_FAILED, "app setting mapper is not available"));
    }

    /**
     * 转换为设置详情 VO。
     *
     * @param settings 配置 map
     * @return 设置详情 VO
     */
    private SettingDetailVO toDetail(Map<String, String> settings) {
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        String plainKey = StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
        return SettingDetailVO.builder()
                .provider(firstText(settings.get(KEY_PROVIDER), DEFAULT_PROVIDER))
                .baseUrl(firstText(settings.get(KEY_BASE_URL), DEFAULT_BASE_URL))
                .apiKeyMasked(secretCipher.mask(plainKey))
                .model(firstText(settings.get(KEY_MODEL), DEFAULT_MODEL))
                .maxContextSize(parseInteger(settings.get(KEY_MAX_CONTEXT_SIZE), DEFAULT_MAX_CONTEXT_SIZE))
                .temperature(parseDecimal(settings.get(KEY_TEMPERATURE), DEFAULT_TEMPERATURE))
                .outputLanguage(firstText(settings.get(KEY_OUTPUT_LANGUAGE), DEFAULT_OUTPUT_LANGUAGE))
                .embeddingEnabled(Boolean.parseBoolean(settings.getOrDefault(KEY_EMBEDDING_ENABLED, "false")))
                .configured(StringUtils.hasText(plainKey))
                .build();
    }

    /**
     * 解析测试请求中的 API Key。
     *
     * @param request 测试请求
     * @param settings 已保存配置
     * @return 明文 API Key
     */
    private String resolveApiKey(SettingTestRequest request, Map<String, String> settings) {
        if (StringUtils.hasText(request.getApiKey()) && !secretCipher.isMasked(request.getApiKey())) {
            return request.getApiKey().trim();
        }
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        return StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
    }

    /**
     * 获取第一个非空文本。
     *
     * @param values 候选文本
     * @return 第一个非空文本
     */
    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 获取值或默认值。
     *
     * @param value 值
     * @param defaultValue 默认值
     * @param <T> 值类型
     * @return 非空值
     */
    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * 解析整数配置。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 整数值
     */
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

    /**
     * 解析 decimal 配置。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return decimal 值
     */
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
