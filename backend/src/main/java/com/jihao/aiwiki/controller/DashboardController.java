package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.entity.SourceDocumentDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.IngestTaskMapper;
import com.jihao.aiwiki.mapper.SourceDocumentMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.vo.dashboard.DashboardOverviewVO;
import com.jihao.aiwiki.vo.dashboard.DashboardOverviewVO.ActiveTaskItem;
import com.jihao.aiwiki.vo.dashboard.DashboardOverviewVO.RecentSourceItem;
import com.jihao.aiwiki.vo.dashboard.DashboardOverviewVO.RecentTaskItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dashboard 概览 API 控制器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ObjectProvider<VaultProjectMapper> vaultMapperProvider;
    private final ObjectProvider<WikiPageMapper> wikiPageMapperProvider;
    private final ObjectProvider<IngestTaskMapper> ingestTaskMapperProvider;
    private final ObjectProvider<SourceDocumentMapper> sourceDocumentMapperProvider;

    public DashboardController(ObjectProvider<VaultProjectMapper> vaultMapperProvider,
                               ObjectProvider<WikiPageMapper> wikiPageMapperProvider,
                               ObjectProvider<IngestTaskMapper> ingestTaskMapperProvider,
                               ObjectProvider<SourceDocumentMapper> sourceDocumentMapperProvider) {
        this.vaultMapperProvider = vaultMapperProvider;
        this.wikiPageMapperProvider = wikiPageMapperProvider;
        this.ingestTaskMapperProvider = ingestTaskMapperProvider;
        this.sourceDocumentMapperProvider = sourceDocumentMapperProvider;
    }

    /**
     * 获取 Vault 概览统计。
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview(@RequestParam Long vaultId) {
        VaultProjectMapper vaultMapper = vaultMapperProvider.getIfAvailable();
        WikiPageMapper wikiPageMapper = wikiPageMapperProvider.getIfAvailable();
        IngestTaskMapper ingestTaskMapper = ingestTaskMapperProvider.getIfAvailable();
        SourceDocumentMapper sourceDocumentMapper = sourceDocumentMapperProvider.getIfAvailable();

        if (vaultMapper == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "database not configured");
        }

        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }

        // Wiki 页面总数
        long wikiPageCount = wikiPageMapper != null ? wikiPageMapper.countByVaultId(vaultId) : 0L;

        // 资料总数
        long sourceCount = sourceDocumentMapper != null ? sourceDocumentMapper.countByVaultId(vaultId) : 0L;

        // 任务统计
        long taskCount = ingestTaskMapper != null ? ingestTaskMapper.countPage(vaultId, null) : 0L;
        long activeTaskCount = ingestTaskMapper != null ? ingestTaskMapper.countPage(vaultId, "PROCESSING") : 0L;
        long failedTaskCount = ingestTaskMapper != null ? ingestTaskMapper.countPage(vaultId, "FAILED") : 0L;

        // 当前运行中任务
        ActiveTaskItem activeTask = null;
        if (ingestTaskMapper != null && activeTaskCount > 0) {
            List<IngestTaskDO> activeTasks = ingestTaskMapper.selectPage(vaultId, "PROCESSING", 0L, 1L);
            if (!activeTasks.isEmpty()) {
                IngestTaskDO t = activeTasks.get(0);
                // 尝试获取资料标题
                String sourceTitle = "Source #" + (t.getSourceId() != null ? t.getSourceId() : "");
                if (sourceDocumentMapper != null && t.getSourceId() != null) {
                    SourceDocumentDO src = sourceDocumentMapper.selectById(t.getSourceId());
                    if (src != null && src.getTitle() != null) {
                        sourceTitle = src.getTitle();
                    }
                }
                activeTask = ActiveTaskItem.builder()
                        .taskId(t.getTaskId())
                        .sourceId(t.getSourceId())
                        .sourceTitle(sourceTitle)
                        .status(t.getStatus())
                        .stage(t.getStage())
                        .progress(t.getProgress())
                        .retryCount(t.getRetryCount())
                        .startedAt(t.getStartedAt())
                        .updateTime(t.getUpdateTime())
                        .build();
            }
        }

        // 最近 5 条资料
        List<RecentSourceItem> recentSources = List.of();
        if (sourceDocumentMapper != null) {
            recentSources = sourceDocumentMapper.selectPage(vaultId, null, null, 0, 5).stream()
                    .map(s -> RecentSourceItem.builder()
                            .id(s.getId())
                            .title(s.getTitle())
                            .type(s.getType())
                            .status(s.getStatus())
                            .originalPath(s.getOriginalPath())
                            .createTime(s.getCreateTime())
                            .updateTime(s.getUpdateTime())
                            .build())
                    .toList();
        }

        // 最近 5 条任务
        List<RecentTaskItem> recentTasks = List.of();
        if (ingestTaskMapper != null) {
            recentTasks = ingestTaskMapper.selectPage(vaultId, null, 0L, 5L).stream()
                    .map(t -> RecentTaskItem.builder()
                            .id(t.getId())
                            .status(t.getStatus())
                            .stage(t.getStage())
                            .createTime(t.getCreateTime())
                            .updateTime(t.getUpdateTime())
                            .build())
                    .toList();
        }

        DashboardOverviewVO vo = DashboardOverviewVO.builder()
                .vaultId(vaultId)
                .vaultName(vault.getName())
                .wikiPageCount(wikiPageCount)
                .sourceCount(sourceCount)
                .taskCount(taskCount)
                .activeTaskCount(activeTaskCount)
                .failedTaskCount(failedTaskCount)
                .lastIndexedAt(vault.getLastIndexedAt())
                .activeTask(activeTask)
                .recentSources(recentSources)
                .recentTasks(recentTasks)
                .build();

        return ApiResponse.success(vo);
    }
}
