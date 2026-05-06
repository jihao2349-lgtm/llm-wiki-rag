package com.jihao.aiwiki.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 客户端配置。
 * 用于轻量队列锁、运行态进度缓存和 SSE 状态。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Configuration
public class RedisConfig {

    /**
     * 创建字符串 Redis Template。
     *
     * @return 字符串 Redis Template
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
