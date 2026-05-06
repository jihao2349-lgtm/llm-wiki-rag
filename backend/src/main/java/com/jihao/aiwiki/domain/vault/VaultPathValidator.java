package com.jihao.aiwiki.domain.vault;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Vault 路径安全校验器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class VaultPathValidator {

    /** Windows 盘符路径前缀 */
    private static final String WINDOWS_DRIVE_PATTERN = "^[A-Za-z]:.*";

    /**
     * 校验 Vault 根目录并返回真实路径。
     *
     * @param vaultPath Vault 根目录
     * @return Vault 真实路径
     */
    public Path validateVaultRoot(Path vaultPath) {
        if (vaultPath == null) {
            throw unsafe("vault path is required");
        }
        try {
            Path normalizedPath = vaultPath.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(normalizedPath)) {
                throw unsafe("vault path must not be a symlink");
            }
            Path realPath = normalizedPath.toRealPath(LinkOption.NOFOLLOW_LINKS).toRealPath();
            if (!Files.isDirectory(realPath) || !Files.isReadable(realPath) || !Files.isWritable(realPath)) {
                throw unsafe("vault path must be a readable and writable directory");
            }
            return realPath;
        } catch (IOException | SecurityException exception) {
            throw unsafe("vault path is not accessible");
        }
    }

    /**
     * 校验 Vault 内相对路径。
     *
     * @param relativePath Vault 内相对路径
     * @return 规范化相对路径
     */
    public Path validateRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw unsafe("relative path is required");
        }
        if (relativePath.indexOf('\0') >= 0) {
            throw unsafe("path contains null byte");
        }
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")
                || relativePath.matches(WINDOWS_DRIVE_PATTERN)) {
            throw unsafe("absolute path is not allowed");
        }
        if (relativePath.contains("\\")) {
            throw unsafe("backslash path is not allowed");
        }
        try {
            Path path = Path.of(relativePath).normalize();
            if (path.isAbsolute() || path.toString().isBlank()) {
                throw unsafe("absolute path is not allowed");
            }
            for (Path segment : path) {
                if ("..".equals(segment.toString())) {
                    throw unsafe("path traversal is not allowed");
                }
            }
            return path;
        } catch (InvalidPathException exception) {
            throw unsafe("invalid path");
        }
    }

    /**
     * 将相对路径解析到 Vault 内，并拒绝软链逃逸。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对路径
     * @return Vault 内目标路径
     */
    public Path resolveInsideVault(Path vaultRoot, String relativePath) {
        Path realRoot = validateVaultRoot(vaultRoot);
        Path safeRelativePath = validateRelativePath(relativePath);
        Path targetPath = realRoot.resolve(safeRelativePath).normalize();
        if (!targetPath.startsWith(realRoot)) {
            throw unsafe("path escapes vault");
        }
        assertNoSymlinkEscape(realRoot, targetPath);
        return targetPath;
    }

    /**
     * 确认已存在的路径段不会通过软链逃逸 Vault。
     *
     * @param realRoot Vault 真实路径
     * @param targetPath 目标路径
     */
    private void assertNoSymlinkEscape(Path realRoot, Path targetPath) {
        Path currentPath = realRoot;
        Path relativeTarget = realRoot.relativize(targetPath);
        for (Path segment : relativeTarget) {
            currentPath = currentPath.resolve(segment);
            if (Files.exists(currentPath)) {
                try {
                    Path realCurrentPath = currentPath.toRealPath();
                    if (!realCurrentPath.startsWith(realRoot)) {
                        throw unsafe("symlink escapes vault");
                    }
                } catch (IOException | SecurityException exception) {
                    throw unsafe("path is not accessible");
                }
            }
        }
    }

    /**
     * 创建路径安全异常。
     *
     * @param message 异常消息
     * @return 业务异常
     */
    private BusinessException unsafe(String message) {
        return new BusinessException(ErrorCode.VAULT_PATH_UNSAFE, message);
    }
}
