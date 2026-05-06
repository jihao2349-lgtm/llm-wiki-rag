package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskEventBroadcaster;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskRecoveryPolicy;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStage;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStatus;
import com.jihao.aiwiki.domain.ingest.queue.VaultIngestLockManager;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.mapper.IngestTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 摄入任务服务测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class IngestTaskServiceImplTest {

    /**
     * 取消 PENDING 任务应成功。
     */
    @Test
    void shouldCancelPendingTask() {
        FakeIngestTaskMapper mapper = new FakeIngestTaskMapper();
        mapper.put(task("task-1", IngestTaskStatus.PENDING, IngestTaskStage.PENDING));
        IngestTaskServiceImpl service = service(mapper);

        assertEquals(IngestTaskStatus.CANCELLED.name(), service.cancelTask("task-1").getStatus());
    }

    /**
     * WRITING 阶段不能自动取消。
     */
    @Test
    void shouldRejectCancelWritingTask() {
        FakeIngestTaskMapper mapper = new FakeIngestTaskMapper();
        mapper.put(task("task-2", IngestTaskStatus.PROCESSING, IngestTaskStage.WRITING));
        IngestTaskServiceImpl service = service(mapper);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.cancelTask("task-2"));
        assertEquals(ErrorCode.TASK_STATE_INVALID, exception.getErrorCode());
    }

    /**
     * 创建服务。
     *
     * @param mapper fake mapper
     * @return 服务
     */
    private IngestTaskServiceImpl service(FakeIngestTaskMapper mapper) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        VaultIngestLockManager lockManager =
                new VaultIngestLockManager(beanFactory.getBeanProvider(StringRedisTemplate.class));
        return new IngestTaskServiceImpl(mapper, lockManager, new IngestTaskEventBroadcaster(),
                new IngestTaskRecoveryPolicy(), context -> {
                }, Runnable::run);
    }

    /**
     * 创建任务。
     *
     * @param taskId 任务 ID
     * @param status 状态
     * @param stage 阶段
     * @return 任务
     */
    private IngestTaskDO task(String taskId, IngestTaskStatus status, IngestTaskStage stage) {
        IngestTaskDO task = new IngestTaskDO();
        task.setTaskId(taskId);
        task.setVaultId(1L);
        task.setSourceId(1L);
        task.setStatus(status.name());
        task.setStage(stage.name());
        task.setProgress(0);
        task.setRetryCount(0);
        return task;
    }

    /**
     * 内存 Mapper。
     */
    private static class FakeIngestTaskMapper implements IngestTaskMapper {

        /** 任务存储 */
        private final Map<String, IngestTaskDO> tasks = new LinkedHashMap<>();

        /**
         * 放入任务。
         *
         * @param task 任务
         */
        void put(IngestTaskDO task) {
            tasks.put(task.getTaskId(), task);
        }

        @Override
        public int insert(IngestTaskDO task) {
            tasks.put(task.getTaskId(), task);
            return 1;
        }

        @Override
        public IngestTaskDO selectByTaskId(String taskId) {
            return tasks.get(taskId);
        }

        @Override
        public List<IngestTaskDO> selectPage(Long vaultId, String status, Long offset, Long limit) {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public Long countPage(Long vaultId, String status) {
            return (long) tasks.size();
        }

        @Override
        public IngestTaskDO selectNextPending(Long vaultId) {
            return tasks.values().stream()
                    .filter(task -> vaultId.equals(task.getVaultId()))
                    .filter(task -> IngestTaskStatus.PENDING.name().equals(task.getStatus()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Long> selectPendingVaultIds() {
            return List.of();
        }

        @Override
        public List<IngestTaskDO> selectHeartbeatTimedOut(LocalDateTime cutoff) {
            return List.of();
        }

        @Override
        public int markProcessing(String taskId, LocalDateTime startedAt, LocalDateTime heartbeatAt) {
            IngestTaskDO task = tasks.get(taskId);
            if (task == null || !IngestTaskStatus.PENDING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(IngestTaskStatus.PROCESSING.name());
            task.setStage(IngestTaskStage.PARSING.name());
            task.setStartedAt(startedAt);
            task.setHeartbeatAt(heartbeatAt);
            return 1;
        }

        @Override
        public int updateProgress(String taskId, String stage, Integer progress, LocalDateTime heartbeatAt) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStage(stage);
            task.setProgress(progress);
            task.setHeartbeatAt(heartbeatAt);
            return 1;
        }

        @Override
        public int updateWrittenFiles(String taskId, String writtenFiles, LocalDateTime heartbeatAt) {
            IngestTaskDO task = tasks.get(taskId);
            task.setWrittenFiles(writtenFiles);
            task.setHeartbeatAt(heartbeatAt);
            return 1;
        }

        @Override
        public int markDone(String taskId, LocalDateTime finishedAt) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStatus(IngestTaskStatus.DONE.name());
            task.setStage(IngestTaskStage.DONE.name());
            task.setFinishedAt(finishedAt);
            return 1;
        }

        @Override
        public int markFailed(String taskId, String errorMessage, LocalDateTime finishedAt) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStatus(IngestTaskStatus.FAILED.name());
            task.setStage(IngestTaskStage.FAILED.name());
            task.setErrorMessage(errorMessage);
            task.setFinishedAt(finishedAt);
            return 1;
        }

        @Override
        public int markManualCheck(String taskId, String errorMessage, LocalDateTime finishedAt) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStatus(IngestTaskStatus.MANUAL_CHECK.name());
            task.setStage(IngestTaskStage.MANUAL_CHECK.name());
            task.setErrorMessage(errorMessage);
            task.setFinishedAt(finishedAt);
            return 1;
        }

        @Override
        public int markRetryPending(String taskId, String errorMessage) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStatus(IngestTaskStatus.PENDING.name());
            task.setStage(IngestTaskStage.PENDING.name());
            task.setRetryCount(task.getRetryCount() + 1);
            task.setErrorMessage(errorMessage);
            return 1;
        }

        @Override
        public int markRecoveredPending(String taskId, String errorMessage) {
            IngestTaskDO task = tasks.get(taskId);
            task.setStatus(IngestTaskStatus.PENDING.name());
            task.setStage(IngestTaskStage.PENDING.name());
            task.setErrorMessage(errorMessage);
            return 1;
        }

        @Override
        public int cancelPending(String taskId, LocalDateTime finishedAt) {
            IngestTaskDO task = tasks.get(taskId);
            if (task == null || !IngestTaskStatus.PENDING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(IngestTaskStatus.CANCELLED.name());
            task.setFinishedAt(finishedAt);
            return 1;
        }

        @Override
        public int cancelProcessing(String taskId, LocalDateTime finishedAt) {
            IngestTaskDO task = tasks.get(taskId);
            if (task == null || !IngestTaskStatus.PROCESSING.name().equals(task.getStatus())
                    || IngestTaskStage.WRITING.name().equals(task.getStage())) {
                return 0;
            }
            task.setStatus(IngestTaskStatus.CANCELLED.name());
            task.setFinishedAt(finishedAt);
            return 1;
        }

        @Override
        public int retryTask(String taskId) {
            IngestTaskDO task = tasks.get(taskId);
            if (task == null) {
                return 0;
            }
            task.setStatus(IngestTaskStatus.PENDING.name());
            task.setStage(IngestTaskStage.PENDING.name());
            return 1;
        }
    }
}
