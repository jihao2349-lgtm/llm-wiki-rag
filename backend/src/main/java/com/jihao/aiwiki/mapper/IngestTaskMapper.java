package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.IngestTaskDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 摄入任务 Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface IngestTaskMapper {

    /**
     * 插入任务。
     *
     * @param task 任务
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO ingest_task (
              task_id, vault_id, source_id, status, stage, progress, retry_count,
              error_message, written_files, started_at, heartbeat_at, finished_at
            ) VALUES (
              #{taskId}, #{vaultId}, #{sourceId}, #{status}, #{stage}, #{progress}, #{retryCount},
              #{errorMessage}, #{writtenFiles}, #{startedAt}, #{heartbeatAt}, #{finishedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(IngestTaskDO task);

    /**
     * 按任务 ID 查询。
     *
     * @param taskId 任务 ID
     * @return 任务
     */
    @Select("""
            SELECT id, task_id, vault_id, source_id, status, stage, progress, retry_count,
                   error_message, written_files, started_at, heartbeat_at, finished_at,
                   create_time, update_time
            FROM ingest_task
            WHERE task_id = #{taskId}
            """)
    IngestTaskDO selectByTaskId(@Param("taskId") String taskId);

    /**
     * 分页查询任务。
     *
     * @param vaultId Vault ID
     * @param status 任务状态
     * @param offset 偏移量
     * @param limit 条数
     * @return 任务列表
     */
    @Select("""
            <script>
            SELECT id, task_id, vault_id, source_id, status, stage, progress, retry_count,
                   error_message, written_files, started_at, heartbeat_at, finished_at,
                   create_time, update_time
            FROM ingest_task
            WHERE 1 = 1
            <if test="vaultId != null">AND vault_id = #{vaultId}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            ORDER BY create_time DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<IngestTaskDO> selectPage(@Param("vaultId") Long vaultId,
                                  @Param("status") String status,
                                  @Param("offset") Long offset,
                                  @Param("limit") Long limit);

    /**
     * 统计任务。
     *
     * @param vaultId Vault ID
     * @param status 任务状态
     * @return 总数
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM ingest_task
            WHERE 1 = 1
            <if test="vaultId != null">AND vault_id = #{vaultId}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            </script>
            """)
    Long countPage(@Param("vaultId") Long vaultId, @Param("status") String status);

    /**
     * 查询下一个等待任务。
     *
     * @param vaultId Vault ID
     * @return 任务
     */
    @Select("""
            SELECT id, task_id, vault_id, source_id, status, stage, progress, retry_count,
                   error_message, written_files, started_at, heartbeat_at, finished_at,
                   create_time, update_time
            FROM ingest_task
            WHERE vault_id = #{vaultId} AND status = 'PENDING'
            ORDER BY create_time ASC, id ASC
            LIMIT 1
            """)
    IngestTaskDO selectNextPending(@Param("vaultId") Long vaultId);

    /**
     * 查询等待任务的 Vault ID。
     *
     * @return Vault ID 列表
     */
    @Select("SELECT DISTINCT vault_id FROM ingest_task WHERE status = 'PENDING'")
    List<Long> selectPendingVaultIds();

    /**
     * 查询心跳超时的执行中任务。
     *
     * @param cutoff 截止时间
     * @return 任务列表
     */
    @Select("""
            SELECT id, task_id, vault_id, source_id, status, stage, progress, retry_count,
                   error_message, written_files, started_at, heartbeat_at, finished_at,
                   create_time, update_time
            FROM ingest_task
            WHERE status = 'PROCESSING'
              AND (heartbeat_at IS NULL OR heartbeat_at < #{cutoff})
            ORDER BY update_time ASC, id ASC
            """)
    List<IngestTaskDO> selectHeartbeatTimedOut(@Param("cutoff") LocalDateTime cutoff);

    /**
     * 将等待任务标记为执行中。
     *
     * @param taskId 任务 ID
     * @param startedAt 开始时间
     * @param heartbeatAt 心跳时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'PROCESSING',
                stage = 'PARSING',
                progress = 5,
                error_message = NULL,
                started_at = #{startedAt},
                heartbeat_at = #{heartbeatAt},
                finished_at = NULL
            WHERE task_id = #{taskId} AND status = 'PENDING'
            """)
    int markProcessing(@Param("taskId") String taskId,
                       @Param("startedAt") LocalDateTime startedAt,
                       @Param("heartbeatAt") LocalDateTime heartbeatAt);

    /**
     * 更新任务进度。
     *
     * @param taskId 任务 ID
     * @param stage 阶段
     * @param progress 进度
     * @param heartbeatAt 心跳时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET stage = #{stage},
                progress = #{progress},
                heartbeat_at = #{heartbeatAt}
            WHERE task_id = #{taskId} AND status = 'PROCESSING'
            """)
    int updateProgress(@Param("taskId") String taskId,
                       @Param("stage") String stage,
                       @Param("progress") Integer progress,
                       @Param("heartbeatAt") LocalDateTime heartbeatAt);

    /**
     * 更新已写入文件 JSON。
     *
     * @param taskId 任务 ID
     * @param writtenFiles 已写入文件 JSON
     * @param heartbeatAt 心跳时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET written_files = #{writtenFiles},
                heartbeat_at = #{heartbeatAt}
            WHERE task_id = #{taskId} AND status = 'PROCESSING'
            """)
    int updateWrittenFiles(@Param("taskId") String taskId,
                           @Param("writtenFiles") String writtenFiles,
                           @Param("heartbeatAt") LocalDateTime heartbeatAt);

    /**
     * 标记任务完成。
     *
     * @param taskId 任务 ID
     * @param finishedAt 完成时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'DONE',
                stage = 'DONE',
                progress = 100,
                heartbeat_at = #{finishedAt},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId} AND status = 'PROCESSING'
            """)
    int markDone(@Param("taskId") String taskId, @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 标记任务失败。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @param finishedAt 完成时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'FAILED',
                stage = 'FAILED',
                error_message = #{errorMessage},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId} AND status = 'PROCESSING'
            """)
    int markFailed(@Param("taskId") String taskId,
                   @Param("errorMessage") String errorMessage,
                   @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 标记任务需要人工检查。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @param finishedAt 完成时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'MANUAL_CHECK',
                stage = 'MANUAL_CHECK',
                error_message = #{errorMessage},
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId}
            """)
    int markManualCheck(@Param("taskId") String taskId,
                        @Param("errorMessage") String errorMessage,
                        @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 自动重试，将任务重新放回等待队列。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'PENDING',
                stage = 'PENDING',
                progress = 0,
                retry_count = retry_count + 1,
                error_message = #{errorMessage},
                started_at = NULL,
                heartbeat_at = NULL,
                finished_at = NULL
            WHERE task_id = #{taskId}
            """)
    int markRetryPending(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    /**
     * 恢复为等待任务。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'PENDING',
                stage = 'PENDING',
                progress = 0,
                error_message = #{errorMessage},
                started_at = NULL,
                heartbeat_at = NULL,
                finished_at = NULL
            WHERE task_id = #{taskId}
            """)
    int markRecoveredPending(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    /**
     * 取消等待任务。
     *
     * @param taskId 任务 ID
     * @param finishedAt 完成时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'CANCELLED',
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId} AND status = 'PENDING'
            """)
    int cancelPending(@Param("taskId") String taskId, @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 取消执行中任务。
     *
     * @param taskId 任务 ID
     * @param finishedAt 完成时间
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'CANCELLED',
                finished_at = #{finishedAt}
            WHERE task_id = #{taskId} AND status = 'PROCESSING' AND stage != 'WRITING'
            """)
    int cancelProcessing(@Param("taskId") String taskId, @Param("finishedAt") LocalDateTime finishedAt);

    /**
     * 手动重试失败任务。
     *
     * @param taskId 任务 ID
     * @return 影响行数
     */
    @Update("""
            UPDATE ingest_task
            SET status = 'PENDING',
                stage = 'PENDING',
                progress = 0,
                error_message = NULL,
                started_at = NULL,
                heartbeat_at = NULL,
                finished_at = NULL
            WHERE task_id = #{taskId} AND status IN ('FAILED', 'MANUAL_CHECK', 'CANCELLED')
            """)
    int retryTask(@Param("taskId") String taskId);
}
