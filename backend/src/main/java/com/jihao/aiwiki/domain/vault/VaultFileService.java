package com.jihao.aiwiki.domain.vault;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Vault 文件系统访问服务。
 * 所有方法只接受 Vault 内相对路径，访问前统一做 canonical path 校验。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class VaultFileService {

    /** history 目录时间戳格式 */
    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /** 路径校验器 */
    private final VaultPathValidator pathValidator;

    /** 系统时钟 */
    private final Clock clock;

    /**
     * 创建 Vault 文件访问服务。
     *
     * @param pathValidator 路径校验器
     */
    @Autowired
    public VaultFileService(VaultPathValidator pathValidator) {
        this(pathValidator, Clock.systemDefaultZone());
    }

    /**
     * 创建 Vault 文件访问服务。
     *
     * @param pathValidator 路径校验器
     * @param clock 系统时钟
     */
    public VaultFileService(VaultPathValidator pathValidator, Clock clock) {
        this.pathValidator = pathValidator;
        this.clock = clock;
    }

    /**
     * 读取 UTF-8 文本文件。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对路径
     * @return 文件内容
     */
    public String readString(Path vaultRoot, String relativePath) {
        Path targetPath = pathValidator.resolveInsideVault(vaultRoot, relativePath);
        try {
            return Files.readString(targetPath, StandardCharsets.UTF_8);
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.VAULT_PATH_UNSAFE, "failed to read vault file");
        }
    }

    /**
     * 创建 Vault 内目录。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对目录
     */
    public void createDirectories(Path vaultRoot, String relativePath) {
        Path targetPath = pathValidator.resolveInsideVault(vaultRoot, relativePath);
        try {
            Files.createDirectories(targetPath);
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.VAULT_PATH_UNSAFE, "failed to create vault directory");
        }
    }

    /**
     * 当文件不存在时写入 UTF-8 文本。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对路径
     * @param content 文件内容
     */
    public void writeStringIfAbsent(Path vaultRoot, String relativePath, String content) {
        Path targetPath = pathValidator.resolveInsideVault(vaultRoot, relativePath);
        if (Files.exists(targetPath)) {
            return;
        }
        writeStringAtomically(vaultRoot, relativePath, content);
    }

    /**
     * 原子写入 UTF-8 文本文件。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对路径
     * @param content 文件内容
     */
    public void writeStringAtomically(Path vaultRoot, String relativePath, String content) {
        Path targetPath = pathValidator.resolveInsideVault(vaultRoot, relativePath);
        Path parentPath = targetPath.getParent();
        try {
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }
            Path tmpPath = Files.createTempFile(parentPath, ".aiwiki-", ".tmp");
            Files.writeString(tmpPath, content, StandardCharsets.UTF_8);
            moveReplacing(tmpPath, targetPath);
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.VAULT_PATH_UNSAFE, "failed to write vault file");
        }
    }

    /**
     * 备份已存在文件到 .ai-wiki/history/{timestamp}/。
     *
     * @param vaultRoot Vault 根目录
     * @param relativePath Vault 内相对路径
     * @return 备份文件相对路径；源文件不存在时为空
     */
    public Optional<String> backupIfExists(Path vaultRoot, String relativePath) {
        Path sourcePath = pathValidator.resolveInsideVault(vaultRoot, relativePath);
        if (!Files.exists(sourcePath)) {
            return Optional.empty();
        }
        String timestamp = LocalDateTime.now(clock).format(HISTORY_TIMESTAMP_FORMAT);
        String backupRelativePath = ".ai-wiki/history/" + timestamp + "/" + relativePath;
        Path backupPath = pathValidator.resolveInsideVault(vaultRoot, backupRelativePath);
        try {
            Path backupParentPath = backupPath.getParent();
            if (backupParentPath != null) {
                Files.createDirectories(backupParentPath);
            }
            Files.copy(sourcePath, backupPath, StandardCopyOption.COPY_ATTRIBUTES);
            return Optional.of(backupRelativePath);
        } catch (IOException | SecurityException exception) {
            throw new BusinessException(ErrorCode.VAULT_PATH_UNSAFE, "failed to backup vault file");
        }
    }

    /**
     * 尽量使用原子替换，不支持时退回普通替换。
     *
     * @param sourcePath 临时文件
     * @param targetPath 目标文件
     * @throws IOException 文件移动失败
     */
    private void moveReplacing(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
