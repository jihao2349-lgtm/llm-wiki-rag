package com.jihao.aiwiki.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v0.1 数据库迁移文件测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class MigrationFileTest {

    /** v0.1 必需基础表 */
    private static final List<String> REQUIRED_TABLES = List.of(
            "vault_project",
            "source_document",
            "ingest_task",
            "wiki_page",
            "chat_session",
            "chat_message",
            "app_setting"
    );

    /**
     * 验证首个迁移文件声明所有 v0.1 基础表。
     *
     * @throws IOException 迁移文件读取失败时抛出
     */
    @Test
    void migrationShouldContainRequiredTables() throws IOException {
        String migration = Files.readString(
                Path.of("src/main/resources/db/migration/V001__init_v01_schema.sql"),
                StandardCharsets.UTF_8
        );

        for (String table : REQUIRED_TABLES) {
            assertThat(migration).contains("CREATE TABLE " + table);
        }
    }
}
