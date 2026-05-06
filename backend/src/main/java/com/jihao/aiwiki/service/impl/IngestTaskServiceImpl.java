package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskCancelledException;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskEventBroadcaster;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskEventType;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskHandler;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskProgressReporter;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskRecoveryPolicy;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskRunContext;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStage;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStatus;
import com.jihao.aiwiki.domain.ingest.queue.VaultIngestLockManager;
import com.jihao.aiwiki.dto.task.IngestTaskCreateRequest;
import com.jihao.aiwiki.dto.task.IngestTaskPageQuery;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.mapper.IngestTaskMapper;
import com.jihao.aiwiki.service.IngestTaskService;
import com.jihao.aiwiki.vo.task.IngestTaskVO;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 摄入任务队列服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
public class IngestTaskServiceImpl implements IngestTaskService, IngestTaskProgressReporter {

    /** 自动重试上限 */
    private static final int MAX_AUTO_RETRY_COUNT = 3;

    /** 默认任务执行器 */
    private final ExecutorService ownedExecutor = Executors.newCachedThreadPool();

    /** 摄入任务 Mapper */
    private final IngestTaskMapper ingestTaskMapper;

    /** Vault 锁管理器 */
    private final VaultIngestLockManager lockManager;

    /** SSE 事件广播器 */
    private final IngestTaskEventBroadcaster eventBroadcaster;

    /** 恢复策略 */
    private final IngestTaskRecoveryPolicy recoveryPolicy;

    /** 摄入任务 handler */
    private final IngestTaskHandler ingestTaskHandler;

    /** 异步执行器 */
    private final Executor executor;

    /**
     * 创建摄入任务服务。
     *
     * @param mapperProvider Mapper Provider
     * @param lockManager Vault 锁管理器
     * @param eventBroadcaster SSE 事件广播器
     * @param recoveryPolicy 恢复策略
     * @param handlerProvider handler Provider
     */
    @Autowired
    public IngestTaskServiceImpl(ObjectProvider<IngestTaskMapper> mapperProvider,
                                 VaultIngestLockManager lockManager,
                                 IngestTaskEventBroadcaster eventBroadcaster,
                                 IngestTaskRecoveryPolicy recoveryPolicy,
                                 ObjectProvider<IngestTaskHandler> handlerProvider) {
        this(mapperProvider.getIfAvailable(), lockManager, eventBroadcaster, recoveryPolicy,
                handlerProvider.getIfAvailable(() -> IngestTaskServiceImpl::noopHandler), null);
    }

    /**
     * 测试用构造器。
     *
     * @param ingestTaskMapper 摄入任务 Mapper
     * @param lockManager Vault 锁管理器
     * @param eventBroadcaster SSE 事件广播器
     * @param recoveryPolicy 恢复策略
     * @param ingestTaskHandler 摄入任务 handler
     * @param executor 异步执行器
     */
    IngestTaskServiceImpl(IngestTaskMapper ingestTaskMapper,
                          VaultIngestLockManager lockManager,
                          IngestTaskEventBroadcaster eventBroadcaster,
                          IngestTaskRecoveryPolicy recoveryPolicy,
                          IngestTaskHandler ingestTaskHandler,
                          Executor executor) {
        this.ingestTaskMapper = ingestTaskMapper;
        this.lockManager = lockManager;
        this.eventBroadcaster = eventBroadcaster;
        this.recoveryPolicy = recoveryPolicy;
        this.ingestTaskHandler = ingestTaskHandler;
        this.executor = executor == null ? ownedExecutor : executor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestTaskVO createTask(IngestTaskCreateRequest request) {
        requireMapper();
        IngestTaskDO task = new IngestTaskDO();
        task.setTaskId("task_" + UUID.randomUUID().toString().replace("-", ""));
        task.setVaultId(request.getVaultId());
        task.setSourceId(request.getSourceId());
        task.setStatus(IngestTaskStatus.PENDING.name());
        task.setStage(IngestTaskStage.PENDING.name());
        task.setProgress(0);
        task.setRetryCount(0);
        ingestTaskMapper.insert(task);
        IngestTaskVO taskVO = toVO(task);
        eventBroadcaster.broadcast(IngestTaskEventType.SNAPSHOT, taskVO);
        enqueueVault(request.getVaultId());
        return taskVO;
    }

    @Override
    public PageResult<IngestTaskVO> pageTasks(IngestTaskPageQuery query) {
        requireMapper();
        long pageNo = query.getPageNo() == null ? 1L : query.getPageNo();
        long pageSize = query.getPageSize() == null ? 20L : query.getPageSize();
        long offset = (pageNo - 1L) * pageSize;
        List<IngestTaskVO> records = ingestTaskMapper
                .selectPage(query.getVaultId(), normalizeStatus(query.getStatus()), offset, pageSize)
                .stream()
                .map(this::toVO)
                .toList();
        Long total = ingestTaskMapper.countPage(query.getVaultId(), normalizeStatus(query.getStatus()));
        return new PageResult<>(records, total, pageNo, pageSize);
    }

    @Override
    public IngestTaskVO getTask(String taskId) {
        return toVO(requireTask(taskId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestTaskVO retryTask(String taskId) {
        IngestTaskDO task = requireTask(taskId);
        int updated = ingestTaskMapper.retryTask(taskId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.TASK_STATE_INVALID,
                    "Only FAILED, MANUAL_CHECK, or CANCELLED tasks can be retried");
        }
        IngestTaskDO reloaded = requireTask(taskId);
        eventBroadcaster.broadcast(IngestTaskEventType.SNAPSHOT, toVO(reloaded));
        enqueueVault(task.getVaultId());
        return toVO(reloaded);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestTaskVO cancelTask(String taskId) {
        IngestTaskDO task = requireTask(taskId);
        if (IngestTaskStatus.PROCESSING.name().equals(task.getStatus())
                && IngestTaskStage.WRITING.name().equals(task.getStage())) {
            throw new BusinessException(ErrorCode.TASK_STATE_INVALID,
                    "WRITING task cannot be cancelled automatically; manual check is required");
        }
        LocalDateTime now = LocalDateTime.now();
        int updated = ingestTaskMapper.cancelPending(taskId, now);
        if (updated == 0) {
            updated = ingestTaskMapper.cancelProcessing(taskId, now);
        }
        if (updated == 0) {
            throw new BusinessException(ErrorCode.TASK_STATE_INVALID,
                    "Only PENDING or non-WRITING PROCESSING tasks can be cancelled");
        }
        IngestTaskVO taskVO = toVO(requireTask(taskId));
        eventBroadcaster.broadcast(IngestTaskEventType.ERROR, taskVO);
        return taskVO;
    }

    @Override
    public SseEmitter streamTasks() {
        return eventBroadcaster.register();
    }

    @Override
    public void recoverUnfinishedTasks() {
        if (ingestTaskMapper == null) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minus(IngestTaskRecoveryPolicy.DEFAULT_HEARTBEAT_TIMEOUT);
        for (IngestTaskDO task : ingestTaskMapper.selectHeartbeatTimedOut(cutoff)) {
            if (recoveryPolicy.requiresManualCheck(task)) {
                ingestTaskMapper.markManualCheck(task.getTaskId(),
                        "Task stopped during WRITING stage; manual check required", LocalDateTime.now());
                eventBroadcaster.broadcast(IngestTaskEventType.ERROR, toVO(requireTask(task.getTaskId())));
            } else {
                ingestTaskMapper.markRecoveredPending(task.getTaskId(), "Recovered after worker heartbeat timeout");
                eventBroadcaster.broadcast(IngestTaskEventType.SNAPSHOT, toVO(requireTask(task.getTaskId())));
                enqueueVault(task.getVaultId());
            }
        }
        for (Long vaultId : ingestTaskMapper.selectPendingVaultIds()) {
            enqueueVault(vaultId);
        }
    }

    @Override
    public void updateProgress(String taskId, IngestTaskStage stage, int progress) {
        IngestTaskDO task = requireTask(taskId);
        if (IngestTaskStatus.CANCELLED.name().equals(task.getStatus())) {
            throw new IngestTaskCancelledException(taskId);
        }
        int boundedProgress = Math.max(0, Math.min(99, progress));
        ingestTaskMapper.updateProgress(taskId, stage.name(), boundedProgress, LocalDateTime.now());
        eventBroadcaster.broadcast(IngestTaskEventType.PROGRESS, toVO(requireTask(taskId)));
    }

    @Override
    public void updateWrittenFiles(String taskId, String writtenFiles) {
        IngestTaskDO task = requireTask(taskId);
        if (IngestTaskStatus.CANCELLED.name().equals(task.getStatus())) {
            throw new IngestTaskCancelledException(taskId);
        }
        ingestTaskMapper.updateWrittenFiles(taskId, writtenFiles, LocalDateTime.now());
        eventBroadcaster.broadcast(IngestTaskEventType.PROGRESS, toVO(requireTask(taskId)));
    }

    @Override
    public boolean isCancellationRequested(String taskId) {
        IngestTaskDO task = requireTask(taskId);
        return IngestTaskStatus.CANCELLED.name().equals(task.getStatus());
    }

    /**
     * 应用启动后恢复未完成任务。
     *
     * @param event 应用启动事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        recoverUnfinishedTasks();
    }

    /**
     * 关闭内部执行器。
     */
    @PreDestroy
    public void shutdown() {
        ownedExecutor.shutdownNow();
    }

    /**
     * 将 Vault 放入异步执行队列。
     *
     * @param vaultId Vault ID
     */
    private void enqueueVault(Long vaultId) {
        executor.execute(() -> drainVault(vaultId));
    }

    /**
     * 串行消费单个 Vault 的等待任务。
     *
     * @param vaultId Vault ID
     */
    private void drainVault(Long vaultId) {
        if (ingestTaskMapper == null) {
            return;
        }
        String owner = UUID.randomUUID().toString();
        if (!lockManager.tryLock(vaultId, owner)) {
            return;
        }
        try {
            IngestTaskDO task = ingestTaskMapper.selectNextPending(vaultId);
            while (task != null) {
                processTask(task);
                task = ingestTaskMapper.selectNextPending(vaultId);
            }
        } finally {
            lockManager.unlock(vaultId, owner);
        }
    }

    /**
     * 执行单个任务。
     *
     * @param task 任务
     */
    private void processTask(IngestTaskDO task) {
        LocalDateTime now = LocalDateTime.now();
        if (ingestTaskMapper.markProcessing(task.getTaskId(), now, now) == 0) {
            return;
        }
        eventBroadcaster.broadcast(IngestTaskEventType.PROGRESS, toVO(requireTask(task.getTaskId())));
        try {
            ingestTaskHandler.handle(new IngestTaskRunContext(task, this));
            if (isCancellationRequested(task.getTaskId())) {
                eventBroadcaster.broadcast(IngestTaskEventType.ERROR, toVO(requireTask(task.getTaskId())));
                return;
            }
            ingestTaskMapper.markDone(task.getTaskId(), LocalDateTime.now());
            eventBroadcaster.broadcast(IngestTaskEventType.DONE, toVO(requireTask(task.getTaskId())));
        } catch (IngestTaskCancelledException exception) {
            eventBroadcaster.broadcast(IngestTaskEventType.ERROR, toVO(requireTask(task.getTaskId())));
        } catch (Exception exception) {
            handleTaskFailure(task.getTaskId(), exception);
        }
    }

    /**
     * 处理任务失败。
     *
     * @param taskId 任务 ID
     * @param exception 异常
     */
    private void handleTaskFailure(String taskId, Exception exception) {
        IngestTaskDO current = requireTask(taskId);
        String message = trimError(exception.getMessage());
        if (IngestTaskStage.WRITING.name().equals(current.getStage())) {
            ingestTaskMapper.markManualCheck(taskId, message, LocalDateTime.now());
            eventBroadcaster.broadcast(IngestTaskEventType.ERROR, toVO(requireTask(taskId)));
            return;
        }
        int retryCount = current.getRetryCount() == null ? 0 : current.getRetryCount();
        if (retryCount < MAX_AUTO_RETRY_COUNT) {
            ingestTaskMapper.markRetryPending(taskId, message);
            eventBroadcaster.broadcast(IngestTaskEventType.SNAPSHOT, toVO(requireTask(taskId)));
        } else {
            ingestTaskMapper.markFailed(taskId, message, LocalDateTime.now());
            eventBroadcaster.broadcast(IngestTaskEventType.ERROR, toVO(requireTask(taskId)));
        }
    }

    /**
     * 查询任务，不存在时抛出业务异常。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    private IngestTaskDO requireTask(String taskId) {
        requireMapper();
        IngestTaskDO task = ingestTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Task not found: " + taskId);
        }
        return task;
    }

    /**
     * 校验 Mapper 可用。
     */
    private void requireMapper() {
        if (ingestTaskMapper == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "IngestTaskMapper is not available");
        }
    }

    /**
     * 规范化状态查询条件。
     *
     * @param status 状态
     * @return 大写状态
     */
    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase();
    }

    /**
     * 转换为 VO。
     *
     * @param task 任务 DO
     * @return 任务 VO
     */
    private IngestTaskVO toVO(IngestTaskDO task) {
        IngestTaskVO vo = new IngestTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setVaultId(task.getVaultId());
        vo.setSourceId(task.getSourceId());
        vo.setStatus(task.getStatus());
        vo.setStage(task.getStage());
        vo.setProgress(task.getProgress());
        vo.setRetryCount(task.getRetryCount());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setWrittenFiles(task.getWrittenFiles());
        vo.setStartedAt(task.getStartedAt());
        vo.setHeartbeatAt(task.getHeartbeatAt());
        vo.setFinishedAt(task.getFinishedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    /**
     * 裁剪错误信息，避免超出数据库字段。
     *
     * @param message 错误信息
     * @return 裁剪后的错误信息
     */
    private String trimError(String message) {
        String error = message == null || message.isBlank() ? "Task execution failed" : message;
        return error.length() > 1024 ? error.substring(0, 1024) : error;
    }

    /**
     * 无 T4 handler 时的占位执行器。
     *
     * @param context 任务上下文
     */
    private static void noopHandler(IngestTaskRunContext context) {
        context.updateProgress(IngestTaskStage.INDEXING, 90);
    }
}
