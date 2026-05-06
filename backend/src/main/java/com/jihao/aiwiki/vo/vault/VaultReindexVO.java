package com.jihao.aiwiki.vo.vault;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Vault 重建索引占位结果。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@Schema(description = "Vault 重建索引占位结果")
public class VaultReindexVO {

    /** Vault ID */
    @Schema(description = "Vault ID", example = "1")
    private Long vaultId;

    /** 当前状态 */
    @Schema(description = "当前状态", example = "ACCEPTED")
    private String status;

    /** 说明消息 */
    @Schema(description = "说明消息", example = "reindex placeholder accepted")
    private String message;
}
