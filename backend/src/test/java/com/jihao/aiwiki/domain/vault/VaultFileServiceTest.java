package com.jihao.aiwiki.domain.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Vault 文件访问服务测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class VaultFileServiceTest {

    /** 固定测试时钟 */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-06T01:02:03.456Z"), ZoneId.of("UTC"));

    /** Vault 文件访问服务 */
    private final VaultFileService vaultFileService =
            new VaultFileService(new VaultPathValidator(), FIXED_CLOCK);

    /**
     * 原子写入会创建父目录并替换目标文件。
     *
     * @param tempDir 临时目录
     * @throws Exception 文件系统失败
     */
    @Test
    void writeStringAtomicallyCreatesParentAndReplacesContent(@TempDir Path tempDir) throws Exception {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));

        vaultFileService.writeStringAtomically(vaultRoot, "wiki/index.md", "first");
        vaultFileService.writeStringAtomically(vaultRoot, "wiki/index.md", "second");

        assertEquals("second", Files.readString(vaultRoot.resolve("wiki/index.md")));
    }

    /**
     * 备份已存在文件到 history 目录。
     *
     * @param tempDir 临时目录
     * @throws Exception 文件系统失败
     */
    @Test
    void backupIfExistsCopiesFileToHistory(@TempDir Path tempDir) throws Exception {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));
        vaultFileService.writeStringAtomically(vaultRoot, "wiki/index.md", "content");

        Optional<String> backupPath = vaultFileService.backupIfExists(vaultRoot, "wiki/index.md");

        assertTrue(backupPath.isPresent());
        assertEquals(".ai-wiki/history/20260506010203456/wiki/index.md", backupPath.get());
        assertEquals("content", Files.readString(vaultRoot.resolve(backupPath.get())));
    }
}
