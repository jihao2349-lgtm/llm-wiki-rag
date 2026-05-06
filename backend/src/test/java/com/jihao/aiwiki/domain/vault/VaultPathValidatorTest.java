package com.jihao.aiwiki.domain.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jihao.aiwiki.common.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Vault 路径安全校验测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class VaultPathValidatorTest {

    /** Vault 路径校验器 */
    private final VaultPathValidator validator = new VaultPathValidator();

    /**
     * 合法相对路径会解析到 Vault 内。
     *
     * @param tempDir 临时目录
     * @throws IOException 文件系统失败
     */
    @Test
    void resolveInsideVaultAllowsSafeRelativePath(@TempDir Path tempDir) throws IOException {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));

        Path resolvedPath = validator.resolveInsideVault(vaultRoot, "wiki/concepts/agent-memory.md");

        assertEquals(vaultRoot.toRealPath().resolve("wiki/concepts/agent-memory.md"), resolvedPath);
    }

    /**
     * 路径穿越会被拒绝。
     *
     * @param tempDir 临时目录
     * @throws IOException 文件系统失败
     */
    @Test
    void resolveInsideVaultRejectsTraversal(@TempDir Path tempDir) throws IOException {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));

        assertThrows(BusinessException.class, () -> validator.resolveInsideVault(vaultRoot, "../secret.md"));
        assertThrows(BusinessException.class, () -> validator.resolveInsideVault(vaultRoot, "wiki/../../secret.md"));
    }

    /**
     * 绝对路径和 Windows 盘符会被拒绝。
     *
     * @param tempDir 临时目录
     * @throws IOException 文件系统失败
     */
    @Test
    void resolveInsideVaultRejectsAbsolutePaths(@TempDir Path tempDir) throws IOException {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));

        assertThrows(BusinessException.class, () -> validator.resolveInsideVault(vaultRoot, "/etc/passwd"));
        assertThrows(BusinessException.class, () -> validator.resolveInsideVault(vaultRoot, "C:/Windows/win.ini"));
    }

    /**
     * 空字节会被拒绝。
     *
     * @param tempDir 临时目录
     * @throws IOException 文件系统失败
     */
    @Test
    void resolveInsideVaultRejectsNullByte(@TempDir Path tempDir) throws IOException {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));

        assertThrows(BusinessException.class, () -> validator.resolveInsideVault(vaultRoot, "wiki/a\u0000.md"));
    }

    /**
     * 指向 Vault 外部的软链会被拒绝。
     *
     * @param tempDir 临时目录
     * @throws IOException 文件系统失败
     */
    @Test
    void resolveInsideVaultRejectsSymlinkEscape(@TempDir Path tempDir) throws IOException {
        Path vaultRoot = Files.createDirectory(tempDir.resolve("vault"));
        Path outsideRoot = Files.createDirectory(tempDir.resolve("outside"));
        Path symlinkPath = vaultRoot.resolve("escape");
        try {
            Files.createSymbolicLink(symlinkPath, outsideRoot);
        } catch (UnsupportedOperationException | IOException exception) {
            Assumptions.abort("symbolic links are unavailable in this environment");
        }

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> validator.resolveInsideVault(vaultRoot, "escape/secret.md"));
        assertTrue(exception.getMessage().contains("symlink") || exception.getMessage().contains("accessible"));
    }
}
