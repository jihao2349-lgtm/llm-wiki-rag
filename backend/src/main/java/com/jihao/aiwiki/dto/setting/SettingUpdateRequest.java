package com.jihao.aiwiki.dto.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * LLM 设置更新请求。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "LLM 设置更新请求")
public class SettingUpdateRequest {

    /** Provider 标识 */
    @NotBlank
    @Schema(description = "Provider 标识", example = "openai-compatible")
    private String provider;

    /** OpenAI-compatible API base URL */
    @NotBlank
    @Schema(description = "API Base URL", example = "https://api.openai.com/v1")
    private String baseUrl;

    /** API Key，为空或 masked key 时保留已保存密钥 */
    @Schema(description = "API Key，更新时传新值；为空或 masked key 时保留已保存密钥")
    private String apiKey;

    /** Chat completion 模型名 */
    @NotBlank
    @Schema(description = "模型名称", example = "gpt-4.1-mini")
    private String model;

    /** 最大上下文长度 */
    @Min(1024)
    @Max(200000)
    @Schema(description = "最大上下文长度", example = "32000")
    private Integer maxContextSize;

    /** 采样温度 */
    @DecimalMin("0.0")
    @DecimalMax("2.0")
    @Schema(description = "采样温度", example = "0.20")
    private BigDecimal temperature;

    /** 输出语言 */
    @NotBlank
    @Schema(description = "输出语言", example = "Chinese")
    private String outputLanguage;

    /** v0.1 默认关闭 embedding */
    @Schema(description = "是否启用 embedding", example = "false")
    private Boolean embeddingEnabled;

    /** Embedding API Base URL */
    @Schema(description = "Embedding API Base URL",
            example = "https://dashscope.aliyuncs.com/compatible-mode/v1")
    private String embeddingBaseUrl;

    /** Embedding API Key，为空或 masked key 时保留已保存密钥 */
    @Schema(description = "Embedding API Key")
    private String embeddingApiKey;

    /** Embedding 模型名称 */
    @Schema(description = "Embedding 模型名称", example = "text-embedding-v4")
    private String embeddingModel;

    /** Embedding 向量维度 */
    @Min(1)
    @Max(4096)
    @Schema(description = "Embedding 向量维度", example = "1024")
    private Integer embeddingDimension;

    /** Embedding 批量大小 */
    @Min(1)
    @Max(100)
    @Schema(description = "Embedding 批量大小", example = "25")
    private Integer embeddingBatchSize;
}
