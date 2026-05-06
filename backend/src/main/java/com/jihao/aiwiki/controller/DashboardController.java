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

    private final VaultProjectMapper vaultMapper;
    private final WikiPageMapper wikiPageMapper;
    private final IngestTaskMapper ingestTaskMapper;

    public DashboardController(VaultProjectMapper vaultMapper,
                                WikiPageMapper wikiPageMapper,
                                IngestTaskMapper ingestTaskMapper) {
        this.vaultMapper = vaultMapper;
        this.wikiPageMapper = wikiPageMapper;
        this.ingestTaskMapper = ingestTaskMapper;
    }

    /**
     * 获取 Vault 概览统计。
     */
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVO> overview(@RequestParam Long vaultId) {
        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }

        long wikiPageCount = wikiPageMapper.countByVaultId(vaultId);
        long taskCount = ingestTaskMapper.countPage(vaultId, null);

        List<IngestTaskDO> recentTaskDOs = ingestTaskMapper.selectPage(vaultId, null, 0L, 5L);
        List<RecentTaskItem> recentTasks = recentTaskDOs.stream()
                .map(t -> RecentTaskItem.builder()
                        .id(t.getId())
                        .status(t.getStatus())
                        .stage(t.getStage())
                        .createTime(t.getCreateTime())
                        .updateTime(t.getUpdateTime())
                        .build())
                .toList();

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
