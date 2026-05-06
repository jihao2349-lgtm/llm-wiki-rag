package com.jihao.aiwiki.domain.ingest.queue;

import com.jihao.aiwiki.entity.IngestTaskDO;

/**
 * 摄入任务执行上下文。
 *
 * @author jihao
 * @date 2026/05/06
 */
public class IngestTaskRunContext {

    /** 任务快照 */
    private final IngestTaskDO task;

    /** 进度回调 */
    private final IngestTaskProgressReporter progressReporter;

    /**
     * 创建执行上下文。
     *
     * @param task 任务快照
     * @param progressReporter 进度回调
     */
    public IngestTaskRunContext(IngestTaskDO task, IngestTaskProgressReporter progressReporter) {
        this.task = task;
        this.progressReporter = progressReporter;
    }

    /**
     * 获取任务快照。
     *
     * @return 任务快照
     */
    public IngestTaskDO getTask() {
        return task;
    }

    /**
     * 更新任务进度。
     *
     * @param stage 阶段
     * @param progress 进度
     */
    public void updateProgress(IngestTaskStage stage, int progress) {
        progressReporter.updateProgress(task.getTaskId(), stage, progress);
    }

    /**
     * 更新已写入文件 JSON。
     *
     * @param writtenFiles 已写入文件 JSON
     */
    public void updateWrittenFiles(String writtenFiles) {
        progressReporter.updateWrittenFiles(task.getTaskId(), writtenFiles);
    }

    /**
     * 判断任务是否已被取消。
     *
     * @return 已取消时返回 true
     */
    public boolean isCancellationRequested() {
        return progressReporter.isCancellationRequested(task.getTaskId());
    }
}
