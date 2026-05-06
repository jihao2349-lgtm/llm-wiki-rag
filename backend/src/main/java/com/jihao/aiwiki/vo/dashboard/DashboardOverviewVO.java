package com.jihao.aiwiki.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard 概览 VO，包含 Vault 统计与最近资料、任务。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewVO {

    /** Vault ID */
    private Long vaultId;

    /** Vault 名称 */
    private String vaultName;

    /** Wiki 页面总数 */
    private long wikiPageCount;

    /** 资料总数 */
    private long sourceCount;

    /** 任务总数 */
    private long taskCount;

    /** 最近一次索引时间 */
    private LocalDateTime lastIndexedAt;

    /** 最近 5 条任务摘要 */
    private List<RecentTaskItem> recentTasks;

    /**
     * 最近任务摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTaskItem {
        private Long id;
        private String status;
        private String stage;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
