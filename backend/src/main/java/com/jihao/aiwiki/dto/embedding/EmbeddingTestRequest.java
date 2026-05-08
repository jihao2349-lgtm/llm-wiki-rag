package com.jihao.aiwiki.dto.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Embedding 连通性测试请求。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Schema(description = "Embedding 连通性测试请求")
public class EmbeddingTestRequest {

    @NotBlank
    @Schema(description = "API Base URL")
    private String baseUrl;

    @NotBlank
    @Schema(description = "API Key")
    private String apiKey;

    @NotBlank
    @Schema(description = "模型名称", example = "text-embedding-v3")
    private String model;

    @Schema(description = "向量维度", example = "1024")
    private Integer dimension = 1024;
}
