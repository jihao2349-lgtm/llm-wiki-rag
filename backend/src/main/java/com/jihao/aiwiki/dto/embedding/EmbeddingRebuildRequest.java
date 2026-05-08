package com.jihao.aiwiki.dto.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量向量化触发请求。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Schema(description = "批量向量化请求")
public class EmbeddingRebuildRequest {

    @NotNull
    @Schema(description = "Vault ID")
    private Long vaultId;

    /**
     * pending：只处理未向量化页面；
     * failed：只重试失败页面；
     * all：全部重新向量化。
     */
    @Schema(description = "处理模式", allowableValues = {"pending", "failed", "all"}, example = "pending")
    private String mode = "pending";
}
