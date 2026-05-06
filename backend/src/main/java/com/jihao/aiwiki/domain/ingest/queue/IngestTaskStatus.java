package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务状态。
 *
 * @author jihao
 * @date 2026/05/06
 */
public enum IngestTaskStatus {

    /** 等待执行 */
    PENDING,

    /** 正在执行 */
    PROCESSING,

    /** 执行完成 */
    DONE,

    /** 执行失败 */
    FAILED,

    /** 已取消 */
    CANCELLED,

    /** 写入阶段崩溃，需要人工检查 */
    MANUAL_CHECK
}
