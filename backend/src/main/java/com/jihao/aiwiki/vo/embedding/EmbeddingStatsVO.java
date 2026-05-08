package com.jihao.aiwiki.vo.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量化统计响应。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Builder
@Schema(description = "向量化统计")
public class EmbeddingStatsVO {

    @Schema(description = "总页面数")
    private long total;

    @Schema(description = "已向量化数量")
    private long success;

    @Schema(description = "失败数量")
    private long failed;

    @Schema(description = "待处理数量")
    private long pending;

    @Schema(description = "最近向量化时间")
    private LocalDateTime lastEmbeddedAt;
}
