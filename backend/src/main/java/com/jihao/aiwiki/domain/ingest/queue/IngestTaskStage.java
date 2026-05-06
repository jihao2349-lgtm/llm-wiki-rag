package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务阶段。
 *
 * @author jihao
 * @date 2026/05/06
 */
public enum IngestTaskStage {

    /** 等待领取 */
    PENDING,

    /** 解析资料 */
    PARSING,

    /** AI 阶段一分析 */
    ANALYZING,

    /** AI 阶段二和文件写入 */
    WRITING,

    /** 更新 Wiki 索引 */
    INDEXING,

    /** 执行完成 */
    DONE,

    /** 执行失败 */
    FAILED,

    /** 需要人工检查 */
    MANUAL_CHECK
}
