package com.jihao.aiwiki;

import com.jihao.aiwiki.mapper.AppSettingMapper;
import com.jihao.aiwiki.mapper.ChatMessageMapper;
import com.jihao.aiwiki.mapper.ChatSessionMapper;
import com.jihao.aiwiki.mapper.IngestTaskMapper;
import com.jihao.aiwiki.mapper.SourceDocumentMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 应用上下文冒烟测试。
 * 排除数据库 / 缓存基础设施，用 MockBean 替代 Mapper 层，验证 Spring 上下文可以正常启动。
 *
 * @author jihao
 * @date 2026/05/06
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration",
        "spring.ai.dashscope.api-key=test"
})
class AiWikiApplicationTests {

    @MockBean AppSettingMapper appSettingMapper;
    @MockBean ChatMessageMapper chatMessageMapper;
    @MockBean ChatSessionMapper chatSessionMapper;
    @MockBean IngestTaskMapper ingestTaskMapper;
    @MockBean SourceDocumentMapper sourceDocumentMapper;
    @MockBean VaultProjectMapper vaultProjectMapper;
    @MockBean WikiPageMapper wikiPageMapper;

    /**
     * 验证基础设施关闭时 Spring 应用上下文可以加载。
     */
    @Test
    void contextLoads() {
    }
}
