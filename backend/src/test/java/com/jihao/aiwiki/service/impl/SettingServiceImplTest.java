package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.domain.llm.LlmChatRequest;
import com.jihao.aiwiki.domain.llm.LlmChatResponse;
import com.jihao.aiwiki.domain.llm.LlmClient;
import com.jihao.aiwiki.domain.llm.LlmStreamDelta;
import com.jihao.aiwiki.domain.vault.SecretCipher;
import com.jihao.aiwiki.dto.setting.SettingTestRequest;
import com.jihao.aiwiki.dto.setting.SettingUpdateRequest;
import com.jihao.aiwiki.entity.AppSettingDO;
import com.jihao.aiwiki.mapper.AppSettingMapper;
import com.jihao.aiwiki.vo.setting.SettingDetailVO;
import com.jihao.aiwiki.vo.setting.SettingTestVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Setting service implementation tests.
 *
 * @author jihao
 * @date 2026/05/06
 */
class SettingServiceImplTest {

    /**
     * 验证更新设置后 mapper 中只保存 API Key 密文。
     */
    @Test
    void updateShouldPersistEncryptedApiKeyAndReturnMaskedKey() {
        FakeAppSettingMapper mapper = new FakeAppSettingMapper();
        SettingServiceImpl service = new SettingServiceImpl(Optional.of(mapper), new SecretCipher(), new FakeLlmClient());
        SettingUpdateRequest request = buildUpdateRequest("sk-test-secret");

        SettingDetailVO detail = service.update(request);

        assertThat(detail.getConfigured()).isTrue();
        assertThat(detail.getApiKeyMasked()).contains("****").doesNotContain("test-secret");
        assertThat(mapper.value("llm.api_key_cipher")).doesNotContain("sk-test-secret");
    }

    /**
     * 验证连通性测试使用 mock client 可通过。
     */
    @Test
    void testLlmShouldPassWithMockClient() {
        FakeAppSettingMapper mapper = new FakeAppSettingMapper();
        SettingServiceImpl service = new SettingServiceImpl(Optional.of(mapper), new SecretCipher(), new FakeLlmClient());
        service.update(buildUpdateRequest("sk-test-secret"));

        SettingTestVO result = service.testLlm(new SettingTestRequest());

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("LLM connectivity OK");
        assertThat(result.getModel()).isEqualTo("gpt-test");
    }

    /**
     * 验证 masked key 更新不会覆盖已有密文。
     */
    @Test
    void updateWithMaskedKeyShouldKeepExistingCipher() {
        FakeAppSettingMapper mapper = new FakeAppSettingMapper();
        SettingServiceImpl service = new SettingServiceImpl(Optional.of(mapper), new SecretCipher(), new FakeLlmClient());
        service.update(buildUpdateRequest("sk-test-secret"));
        String existingCipher = mapper.value("llm.api_key_cipher");
        SettingUpdateRequest request = buildUpdateRequest("sk-****cret");

        service.update(request);

        assertThat(mapper.value("llm.api_key_cipher")).isEqualTo(existingCipher);
    }

    /**
     * 创建默认更新请求。
     *
     * @param apiKey API Key
     * @return 更新请求
     */
    private SettingUpdateRequest buildUpdateRequest(String apiKey) {
        SettingUpdateRequest request = new SettingUpdateRequest();
        request.setProvider("openai-compatible");
        request.setBaseUrl("https://example.com/v1");
        request.setApiKey(apiKey);
        request.setModel("gpt-test");
        request.setMaxContextSize(32000);
        request.setTemperature(BigDecimal.valueOf(0.2));
        request.setOutputLanguage("Chinese");
        request.setEmbeddingEnabled(false);
        return request;
    }

    /**
     * In-memory app_setting mapper.
     */
    private static class FakeAppSettingMapper implements AppSettingMapper {

        /** Stored settings */
        private final Map<String, AppSettingDO> settings = new LinkedHashMap<>();

        @Override
        public AppSettingDO findByKey(String settingKey) {
            return settings.get(settingKey);
        }

        @Override
        public List<AppSettingDO> findByKeys(List<String> settingKeys) {
            List<AppSettingDO> result = new ArrayList<>();
            for (String settingKey : settingKeys) {
                AppSettingDO setting = settings.get(settingKey);
                if (setting != null) {
                    result.add(setting);
                }
            }
            return result;
        }

        @Override
        public int insert(AppSettingDO setting) {
            settings.put(setting.getSettingKey(), setting);
            return 1;
        }

        @Override
        public int updateValue(String settingKey, String settingValue) {
            settings.get(settingKey).setSettingValue(settingValue);
            return 1;
        }

        /**
         * 读取配置值。
         *
         * @param settingKey 配置键
         * @return 配置值
         */
        private String value(String settingKey) {
            return settings.get(settingKey).getSettingValue();
        }
    }

    /**
     * Fake LLM client.
     */
    private static class FakeLlmClient implements LlmClient {

        @Override
        public LlmChatResponse chat(LlmChatRequest request) {
            assertThat(request.getApiKey()).isEqualTo("sk-test-secret");
            return new LlmChatResponse(request.getModel(), "OK");
        }

        @Override
        public void streamChat(LlmChatRequest request, Consumer<LlmStreamDelta> deltaConsumer) {
            deltaConsumer.accept(new LlmStreamDelta("OK", false));
            deltaConsumer.accept(new LlmStreamDelta("", true));
        }
    }
}
