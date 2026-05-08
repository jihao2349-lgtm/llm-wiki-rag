package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.service.SettingService;
import com.jihao.aiwiki.domain.embedding.EmbeddingConfig;
import com.jihao.aiwiki.dto.setting.SettingTestRequest;
import com.jihao.aiwiki.dto.setting.SettingUpdateRequest;
import com.jihao.aiwiki.vo.setting.SettingDetailVO;
import com.jihao.aiwiki.vo.setting.SettingTestVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Setting controller tests.
 *
 * @author jihao
 * @date 2026/05/06
 */
class SettingControllerTest {

    /**
     * 验证 detail 接口保持统一响应结构。
     *
     * @throws Exception MVC 调用失败
     */
    @Test
    void detailShouldReturnSharedApiResponse() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new SettingController(new FakeSettingService()))
                .build();

        mockMvc.perform(get("/api/settings/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-****cret"))
                .andExpect(jsonPath("$.data.model").value("gpt-test"));
    }

    /**
     * Fake setting service.
     */
    private static class FakeSettingService implements SettingService {

        @Override
        public SettingDetailVO getDetail() {
            return SettingDetailVO.builder()
                    .provider("openai-compatible")
                    .baseUrl("https://example.com/v1")
                    .apiKeyMasked("sk-****cret")
                    .model("gpt-test")
                    .maxContextSize(32000)
                    .temperature(BigDecimal.valueOf(0.2))
                    .outputLanguage("Chinese")
                    .embeddingEnabled(false)
                    .configured(true)
                    .build();
        }

        @Override
        public SettingDetailVO update(SettingUpdateRequest request) {
            return getDetail();
        }

        @Override
        public SettingTestVO testLlm(SettingTestRequest request) {
            return SettingTestVO.builder().success(true).message("OK").model("gpt-test").build();
        }

        @Override
        public EmbeddingConfig getEmbeddingConfig() {
            return EmbeddingConfig.builder()
                    .enabled(false)
                    .baseUrl(EmbeddingConfig.DEFAULT_BASE_URL)
                    .model(EmbeddingConfig.DEFAULT_MODEL)
                    .dimension(EmbeddingConfig.DEFAULT_DIMENSION)
                    .batchSize(EmbeddingConfig.DEFAULT_BATCH_SIZE)
                    .build();
        }
    }
}
