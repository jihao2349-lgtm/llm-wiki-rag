package com.jihao.aiwiki.domain.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量化任务进度（存入 Redis）。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingProgress {

    private boolean processing;
    private int current;
    private int total;

    public static EmbeddingProgress idle() {
        return new EmbeddingProgress(false, 0, 0);
    }

    public static EmbeddingProgress running(int current, int total) {
        return new EmbeddingProgress(true, current, total);
    }
}
