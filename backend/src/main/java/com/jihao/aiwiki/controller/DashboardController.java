package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.IngestTaskMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.vo.dashboard.DashboardOverviewVO;
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

    public DashboardController(ObjectProvider<VaultProjectMapper> vaultMapperProvider,
                                ObjectProvider<WikiPageMapper> wikiPageMapperProvider,
                                ObjectProvider<IngestTaskMapper> ingestTaskMapperProvider) {
        this.vaultMapperProvider = vaultMapperProvider;
        this.wikiPageMapperProvider = wikiPageMapperProvider;
        this.ingestTaskMapperProvider = ingestTaskMapperProvider;
    }

    /**
     * 获取 Vault 概览统计。
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview(@RequestParam Long vaultId) {
        VaultProjectMapper vaultMapper = vaultMapperProvider.getIfAvailable();
        WikiPageMapper wikiPageMapper = wikiPageMapperProvider.getIfAvailable();
        IngestTaskMapper ingestTaskMapper = ingestTaskMapperProvider.getIfAvailable();

        if (vaultMapper == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "database not configured");
        }

        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }

        long wikiPageCount = wikiPageMapper != null ? wikiPageMapper.countByVaultId(vaultId) : 0L;
        long taskCount = ingestTaskMapper != null ? ingestTaskMapper.countPage(vaultId, null) : 0L;

        List<RecentTaskItem> recentTasks = List.of();
        if (ingestTaskMapper != null) {
            List<IngestTaskDO> recentTaskDOs = ingestTaskMapper.selectPage(vaultId, null, 0L, 5L);
            recentTasks = recentTaskDOs.stream()
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
                .sourceCount(0L)
                .taskCount(taskCount)
                .lastIndexedAt(vault.getLastIndexedAt())
                .recentTasks(recentTasks)
                .build();

        return ApiResponse.success(vo);
    }
}
