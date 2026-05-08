package com.jihao.aiwiki.domain.embedding;

/**
 * 单页向量化结果。
 *
 * @author jihao
 * @date 2026/05/08
 */
public record EmbeddingResult(Long pageId, boolean success, boolean skipped, String error) {

    public static EmbeddingResult success(Long pageId) {
        return new EmbeddingResult(pageId, true, false, null);
    }

    public static EmbeddingResult skipped(Long pageId) {
        return new EmbeddingResult(pageId, true, true, null);
    }

    public static EmbeddingResult failed(Long pageId, String error) {
        return new EmbeddingResult(pageId, false, false, error);
    }
}
