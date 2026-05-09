package com.jihao.aiwiki.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jihao.aiwiki.domain.vault.VaultDirectoryInitializer;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.domain.vault.VaultPathValidator;
import com.jihao.aiwiki.dto.vault.VaultInitDTO;
import com.jihao.aiwiki.dto.vault.VaultReindexDTO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.service.WikiPageService;
import com.jihao.aiwiki.vo.vault.VaultReindexVO;
import com.jihao.aiwiki.vo.wiki.WikiPageDetailVO;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import com.jihao.aiwiki.vo.wiki.WikiTreeNodeVO;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Vault 初始化服务测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class VaultServiceImplTest {

    /** Vault 项目数据库访问 */
    private final FakeVaultProjectMapper vaultProjectMapper = new FakeVaultProjectMapper();

    /** Wiki 页面索引服务 */
    private final FakeWikiPageService wikiPageService = new FakeWikiPageService();

    /** Vault 服务 */
    private final VaultServiceImpl vaultService = new VaultServiceImpl(
            vaultProjectMapper,
            new VaultPathValidator(),
            new VaultDirectoryInitializer(new VaultFileService(new VaultPathValidator())),
            wikiPageService);

    /**
     * 初始化 Vault 不会读取或修改 .obsidian 目录。
     *
     * @param tempDir 临时目录
     * @throws Exception 文件系统失败
     */
    @Test
    void initVaultDoesNotModifyObsidianDirectory(@TempDir Path tempDir) throws Exception {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));
        Path obsidianDirectory = Files.createDirectory(vaultRoot.resolve(".obsidian"));
        Files.writeString(obsidianDirectory.resolve("app.json"), "{\"theme\":\"dark\"}");

        vaultService.initVault(initDTO(vaultRoot));

        assertEquals("{\"theme\":\"dark\"}", Files.readString(obsidianDirectory.resolve("app.json")));
        assertTrue(Files.isDirectory(vaultRoot.resolve("raw/sources")));
        assertTrue(Files.isDirectory(vaultRoot.resolve("raw/assets")));
        assertTrue(Files.isDirectory(vaultRoot.resolve("wiki/sources")));
        assertTrue(Files.isDirectory(vaultRoot.resolve(".ai-wiki/cache")));
        assertEquals(1, vaultProjectMapper.insertCount);
    }

    /**
     * 绑定已有 Vault 不覆盖 purpose.md 和 schema.md。
     *
     * @param tempDir 临时目录
     * @throws Exception 文件系统失败
     */
    @Test
    void initVaultDoesNotOverwriteExistingPurposeAndSchema(@TempDir Path tempDir) throws Exception {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));
        Files.writeString(vaultRoot.resolve("purpose.md"), "existing purpose");
        Files.writeString(vaultRoot.resolve("schema.md"), "existing schema");
        VaultProjectDO existingVault = new VaultProjectDO();
        existingVault.setId(7L);
        existingVault.setPath(vaultRoot.toRealPath().toString());
        vaultProjectMapper.existingVault = existingVault;

        vaultService.initVault(initDTO(vaultRoot));

        assertEquals("existing purpose", Files.readString(vaultRoot.resolve("purpose.md")));
        assertEquals("existing schema", Files.readString(vaultRoot.resolve("schema.md")));
        assertEquals(1, vaultProjectMapper.updateCount);
    }

    /**
     * 重建索引会调用 Wiki 索引服务并更新 Vault 最近索引时间。
     *
     * @param tempDir 临时目录
     * @throws Exception 文件系统失败
     */
    @Test
    void reindexRunsWikiIndexerAndUpdatesLastIndexedAt(@TempDir Path tempDir) throws Exception {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));
        VaultProjectDO existingVault = new VaultProjectDO();
        existingVault.setId(9L);
        existingVault.setName("Indexed Vault");
        existingVault.setPath(vaultRoot.toRealPath().toString());
        existingVault.setPurpose("Test purpose");
        existingVault.setStatus("READY");
        vaultProjectMapper.existingVault = existingVault;
        wikiPageService.indexedCount = 3;

        VaultReindexDTO request = new VaultReindexDTO();
        request.setVaultId(9L);
        VaultReindexVO result = vaultService.reindex(request);

        assertEquals("DONE", result.getStatus());
        assertTrue(result.getMessage().contains("3 wiki pages"));
        assertEquals(1, wikiPageService.reindexCount);
        assertEquals(vaultRoot.toRealPath().toString(), wikiPageService.lastVaultPath);
        assertEquals(1, vaultProjectMapper.updateLastIndexedAtCount);
        assertTrue(existingVault.getLastIndexedAt() != null);
    }

    /**
     * 创建初始化 DTO。
     *
     * @param vaultRoot Vault 根目录
     * @return 初始化 DTO
     */
    private VaultInitDTO initDTO(Path vaultRoot) {
        VaultInitDTO initDTO = new VaultInitDTO();
        initDTO.setPath(vaultRoot.toString());
        initDTO.setName("Test Vault");
        initDTO.setPurpose("Test purpose");
        return initDTO;
    }

    /**
     * 不依赖 Mockito 的 VaultProjectMapper 测试替身。
     */
    private static class FakeVaultProjectMapper implements VaultProjectMapper {

        /** 已存在 Vault */
        private VaultProjectDO existingVault;

        /** 插入次数 */
        private int insertCount;

        /** 更新次数 */
        private int updateCount;

        @Override
        public VaultProjectDO selectById(Long id) {
            return existingVault != null && existingVault.getId().equals(id) ? existingVault : null;
        }

        @Override
        public VaultProjectDO selectByPath(String path) {
            return existingVault != null && existingVault.getPath().equals(path) ? existingVault : null;
        }

        @Override
        public VaultProjectDO selectLatest() {
            return existingVault;
        }

        @Override
        public int insert(VaultProjectDO vaultProject) {
            insertCount++;
            vaultProject.setId(1L);
            existingVault = vaultProject;
            return 1;
        }

        @Override
        public int updateBinding(VaultProjectDO vaultProject) {
            updateCount++;
            existingVault = vaultProject;
            return 1;
        }

        @Override
        public int updateLastIndexedAt(Long id, LocalDateTime lastIndexedAt) {
            updateLastIndexedAtCount++;
            existingVault.setLastIndexedAt(lastIndexedAt);
            return 1;
        }

        /** 最近索引时间更新次数 */
        private int updateLastIndexedAtCount;
    }

    /**
     * WikiPageService 测试替身。
     */
    private static class FakeWikiPageService implements WikiPageService {

        /** 重建索引调用次数 */
        private int reindexCount;

        /** 最近一次 Vault 路径 */
        private String lastVaultPath;

        /** 返回的索引数量 */
        private int indexedCount;

        @Override
        public List<WikiTreeNodeVO> tree(Long vaultId) {
            return List.of();
        }

        @Override
        public WikiPageDetailVO page(Long vaultId, String path) {
            return null;
        }

        @Override
        public List<WikiSearchResultVO> search(Long vaultId, String query) {
            return List.of();
        }

        @Override
        public int reindex(Long vaultId, String vaultPath) {
            reindexCount++;
            lastVaultPath = vaultPath;
            return indexedCount;
        }
    }
}
