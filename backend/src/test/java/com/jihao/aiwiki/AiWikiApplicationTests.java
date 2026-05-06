package com.jihao.aiwiki;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文冒烟测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "spring.ai.dashscope.api-key=test"
})
class AiWikiApplicationTests {

    /**
     * 验证基础设施自动配置关闭时 Spring 应用上下文可以加载。
     */
    @Test
    void contextLoads() {
    }
}
