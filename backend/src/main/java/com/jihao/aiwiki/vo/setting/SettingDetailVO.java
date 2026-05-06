package com.jihao.aiwiki.vo.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * LLM 设置详情。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@Schema(description = "LLM 设置详情")
public class SettingDetailVO {

    /** Provider 标识 */
    @Schema(description = "Provider 标识")
    private String provider;

    /** API Base URL */
    @Schema(description = "API Base URL")
    private String baseUrl;

    /** Masked API Key */
    @Schema(description = "脱敏后的 API Key")
    private String apiKeyMasked;

    /** 模型名称 */
    @Schema(description = "模型名称")
    private String model;

    /** 最大上下文长度 */
    @Schema(description = "最大上下文长度")
    private Integer maxContextSize;

    /** 采样温度 */
    @Schema(description = "采样温度")
    private BigDecimal temperature;

    /** 输出语言 */
    @Schema(description = "输出语言")
    private String outputLanguage;

    /** 是否启用 embedding */
    @Schema(description = "是否启用 embedding")
    private Boolean embeddingEnabled;

    /** 是否已有可用 API Key */
    @Schema(description = "是否已有可用 API Key")
    private Boolean configured;
}
