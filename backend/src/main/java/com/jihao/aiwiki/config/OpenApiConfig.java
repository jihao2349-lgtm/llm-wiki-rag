package com.jihao.aiwiki.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Configuration
public class OpenApiConfig {

    /**
     * 创建 OpenAPI 元数据 Bean。
     *
     * @return OpenAPI 元数据
     */
    @Bean
    public OpenAPI aiWikiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Obsidian Wiki API")
                        .version("v0.1")
                        .description("API contract for the AI Obsidian Wiki MVP."));
    }
}
