package com.jihao.aiwiki.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄入任务视图对象。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "摄入任务")
public class IngestTaskVO {

    /** 业务任务 ID */
    @Schema(description = "业务任务 ID")
    private String taskId;

    /** Vault ID */
    @Schema(description = "Vault ID")
    private Long vaultId;

    /** 资料 ID */
    @Schema(description = "资料 ID")
    private Long sourceId;

    /** 任务状态 */
    @Schema(description = "任务状态")
    private String status;

    /** 执行阶段 */
    @Schema(description = "执行阶段")
    private String stage;

    /** 执行进度 */
    @Schema(description = "执行进度")
    private Integer progress;

    /** 已重试次数 */
    @Schema(description = "已重试次数")
    private Integer retryCount;

    /** 错误信息 */
    @Schema(description = "错误信息")
    private String errorMessage;

    /** 已写入文件 JSON */
    @Schema(description = "已写入文件 JSON")
    private String writtenFiles;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    /** Worker 心跳时间 */
    @Schema(description = "Worker 心跳时间")
    private LocalDateTime heartbeatAt;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
