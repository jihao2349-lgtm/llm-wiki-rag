package com.jihao.aiwiki.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄入任务数据库对象。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class IngestTaskDO {

    /** 主键 ID */
    private Long id;

    /** 业务任务 ID */
    private String taskId;

    /** Vault ID */
    private Long vaultId;

    /** 资料 ID */
    private Long sourceId;

    /** 任务状态 */
    private String status;

    /** 执行阶段 */
    private String stage;

    /** 执行进度 */
    private Integer progress;

    /** 已重试次数 */
    private Integer retryCount;

    /** 错误信息 */
    private String errorMessage;

    /** 已写入文件 JSON */
    private String writtenFiles;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** Worker 心跳时间 */
    private LocalDateTime heartbeatAt;

    /** 结束时间 */
    private LocalDateTime finishedAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
