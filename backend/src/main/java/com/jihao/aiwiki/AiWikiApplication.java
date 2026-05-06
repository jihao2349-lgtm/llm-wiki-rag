package com.jihao.aiwiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Obsidian Wiki 后端应用入口。
 *
 * @author jihao
 * @date 2026/05/06
 */
@SpringBootApplication
public class AiWikiApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 运行时命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiWikiApplication.class, args);
    }
}
