package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务 SSE 事件类型。
 *
 * @author jihao
 * @date 2026/05/06
 */
public enum IngestTaskEventType {

    /** 新建或恢复任务快照 */
    SNAPSHOT,

    /** 任务进度变化 */
    PROGRESS,

    /** 任务完成 */
    DONE,

    /** 任务失败或进入人工检查 */
    ERROR
}
