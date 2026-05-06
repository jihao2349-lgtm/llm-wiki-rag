package com.jihao.aiwiki.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 摄入任务分页查询。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "摄入任务分页查询")
public class IngestTaskPageQuery {

    /** Vault ID */
    @Schema(description = "Vault ID", example = "1")
    private Long vaultId;

    /** 任务状态 */
    @Schema(description = "任务状态", example = "PENDING")
    private String status;

    /** 页码 */
    @Min(value = 1, message = "pageNo must be greater than 0")
    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNo = 1L;

    /** 每页条数 */
    @Min(value = 1, message = "pageSize must be greater than 0")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    @Schema(description = "每页条数", example = "20")
    private Long pageSize = 20L;
}
