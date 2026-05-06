package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务进度回调。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface IngestTaskProgressReporter {

    /**
     * 更新任务进度。
     *
     * @param taskId 任务 ID
     * @param stage 阶段
     * @param progress 进度
     */
    void updateProgress(String taskId, IngestTaskStage stage, int progress);

    /**
     * 更新写入文件列表。
     *
     * @param taskId 任务 ID
     * @param writtenFiles 已写入文件 JSON
     */
    void updateWrittenFiles(String taskId, String writtenFiles);

    /**
     * 判断任务是否已被取消。
     *
     * @param taskId 任务 ID
     * @return 已取消时返回 true
     */
    boolean isCancellationRequested(String taskId);
}
