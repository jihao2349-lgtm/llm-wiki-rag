package com.jihao.aiwiki.domain.embedding;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 向量化统计数据。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Builder
public class EmbeddingStats {
    private long total;
    private long success;
    private long failed;
    private long pending;
    private LocalDateTime lastEmbeddedAt;
}
