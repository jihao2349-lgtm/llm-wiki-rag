package com.jihao.aiwiki.domain.ingest.queue;

import com.jihao.aiwiki.entity.IngestTaskDO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 摄入任务恢复策略。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class IngestTaskRecoveryPolicy {

    /** 默认 Worker 心跳超时时间 */
    public static final Duration DEFAULT_HEARTBEAT_TIMEOUT = Duration.ofMinutes(5);

    /**
     * 判断 PROCESSING 任务是否心跳超时。
     *
     * @param task 任务
     * @param now 当前时间
     * @return 超时时返回 true
     */
    public boolean isHeartbeatTimedOut(IngestTaskDO task, LocalDateTime now) {
        if (!IngestTaskStatus.PROCESSING.name().equals(task.getStatus())) {
            return false;
        }
        LocalDateTime heartbeatAt = task.getHeartbeatAt();
        if (heartbeatAt == null) {
            heartbeatAt = task.getStartedAt();
        }
        return heartbeatAt == null || heartbeatAt.plus(DEFAULT_HEARTBEAT_TIMEOUT).isBefore(now);
    }

    /**
     * 判断恢复时是否必须人工检查。
     *
     * @param task 任务
     * @return 写入阶段返回 true
     */
    public boolean requiresManualCheck(IngestTaskDO task) {
        return IngestTaskStage.WRITING.name().equals(task.getStage());
    }
}
