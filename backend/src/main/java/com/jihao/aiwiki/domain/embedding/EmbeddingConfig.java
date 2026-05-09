package com.jihao.aiwiki.domain.embedding;

import lombok.Builder;
import lombok.Data;

/**
 * Embedding 服务配置 POJO，从 SettingService 加载。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Builder
public class EmbeddingConfig {

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
    private int dimension;
    private int batchSize;

    /** 默认 DashScope 地址 */
    public static final String DEFAULT_BASE_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1";

    public static final String DEFAULT_MODEL = "text-embedding-v4";
    public static final int DEFAULT_DIMENSION = 1024;
    public static final int DEFAULT_BATCH_SIZE = 10;
}
