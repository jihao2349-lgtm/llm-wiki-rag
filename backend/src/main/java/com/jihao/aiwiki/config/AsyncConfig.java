package com.jihao.aiwiki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Embedding 批量向量化专用线程池。
     * corePoolSize=2 限制并发 API 调用，避免触发限流。
     */
    @Bean("embeddingExecutor")
    public Executor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("embedding-");
        executor.initialize();
        return executor;
    }
}
