package com.jihao.aiwiki.service;

import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.dto.task.IngestTaskCreateRequest;
import com.jihao.aiwiki.dto.task.IngestTaskPageQuery;
import com.jihao.aiwiki.vo.task.IngestTaskVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 摄入任务队列服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface IngestTaskService {

    /**
     * 创建摄入任务。
     *
     * @param request 创建请求
     * @return 任务
     */
    IngestTaskVO createTask(IngestTaskCreateRequest request);

    /**
     * 分页查询任务。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<IngestTaskVO> pageTasks(IngestTaskPageQuery query);

    /**
     * 查询任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    IngestTaskVO getTask(String taskId);

    /**
     * 重试任务。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    IngestTaskVO retryTask(String taskId);

    /**
     * 取消任务。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    IngestTaskVO cancelTask(String taskId);

    /**
     * 注册任务进度 SSE。
     *
     * @return SSE emitter
     */
    SseEmitter streamTasks();

    /**
     * 恢复启动前未完成任务。
     */
    void recoverUnfinishedTasks();

    /**
     * 清除已终止任务（CANCELLED / FAILED / MANUAL_CHECK）。
     *
     * @param vaultId Vault ID
     * @return 删除条数
     */
    int clearTerminated(Long vaultId);
}
