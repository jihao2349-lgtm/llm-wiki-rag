package com.jihao.aiwiki.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建摄入任务请求。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "创建摄入任务请求")
public class IngestTaskCreateRequest {

    /** Vault ID */
    @NotNull(message = "vaultId is required")
    @Schema(description = "Vault ID", example = "1")
    private Long vaultId;

    /** 资料 ID */
    @NotNull(message = "sourceId is required")
    @Schema(description = "资料 ID", example = "10")
    private Long sourceId;
}
