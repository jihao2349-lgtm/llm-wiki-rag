package com.jihao.aiwiki.dto.vault;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Vault 重建索引请求。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "Vault 重建索引请求")
public class VaultReindexDTO {

    /** Vault ID，为空时使用最近绑定的 Vault */
    @Positive
    @Schema(description = "Vault ID，为空时使用最近绑定的 Vault", example = "1")
    private Long vaultId;
}
