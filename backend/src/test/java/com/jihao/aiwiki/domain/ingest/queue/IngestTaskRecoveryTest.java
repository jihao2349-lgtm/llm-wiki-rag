package com.jihao.aiwiki.domain.ingest.queue;

import com.jihao.aiwiki.entity.IngestTaskDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 摄入任务恢复策略测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class IngestTaskRecoveryTest {

    /** 恢复策略 */
    private final IngestTaskRecoveryPolicy recoveryPolicy = new IngestTaskRecoveryPolicy();

    /**
     * 心跳超时的 PROCESSING 任务需要恢复。
     */
    @Test
    void shouldRecoverHeartbeatTimedOutProcessingTask() {
        IngestTaskDO task = new IngestTaskDO();
        task.setStatus(IngestTaskStatus.PROCESSING.name());
        task.setStage(IngestTaskStage.ANALYZING.name());
        task.setHeartbeatAt(LocalDateTime.now().minusMinutes(6));

        assertTrue(recoveryPolicy.isHeartbeatTimedOut(task, LocalDateTime.now()));
    }

    /**
     * 心跳未超时的 PROCESSING 任务不恢复。
     */
    @Test
    void shouldKeepFreshProcessingTaskRunning() {
        IngestTaskDO task = new IngestTaskDO();
        task.setStatus(IngestTaskStatus.PROCESSING.name());
        task.setStage(IngestTaskStage.ANALYZING.name());
        task.setHeartbeatAt(LocalDateTime.now().minusMinutes(1));

        assertFalse(recoveryPolicy.isHeartbeatTimedOut(task, LocalDateTime.now()));
    }

    /**
     * WRITING 阶段崩溃必须进入人工检查。
     */
    @Test
    void shouldRequireManualCheckForWritingStage() {
        IngestTaskDO task = new IngestTaskDO();
        task.setStage(IngestTaskStage.WRITING.name());

        assertTrue(recoveryPolicy.requiresManualCheck(task));
    }
}
