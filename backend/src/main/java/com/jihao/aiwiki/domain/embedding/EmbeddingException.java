package com.jihao.aiwiki.domain.embedding;

/**
 * Embedding API 调用失败异常。
 *
 * @author jihao
 * @date 2026/05/08
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
