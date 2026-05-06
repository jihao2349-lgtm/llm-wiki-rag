package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务取消异常。
 *
 * @author jihao
 * @date 2026/05/06
 */
public class IngestTaskCancelledException extends RuntimeException {

    /**
     * 创建取消异常。
     *
     * @param taskId 任务 ID
     */
    public IngestTaskCancelledException(String taskId) {
        super("Task " + taskId + " has been cancelled");
    }
}
