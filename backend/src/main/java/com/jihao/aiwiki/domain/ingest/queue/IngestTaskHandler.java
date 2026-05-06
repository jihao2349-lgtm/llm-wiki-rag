package com.jihao.aiwiki.domain.ingest.queue;

/**
 * 摄入任务执行器。
 * T4 负责提供真实实现，T3 只定义可注入的执行契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
@FunctionalInterface
public interface IngestTaskHandler {

    /**
     * 执行摄入任务。
     *
     * @param context 任务上下文
     * @throws Exception 执行失败
     */
    void handle(IngestTaskRunContext context) throws Exception;
}
