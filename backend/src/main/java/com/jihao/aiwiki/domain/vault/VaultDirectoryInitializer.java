package com.jihao.aiwiki.domain.vault;

import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Vault 默认目录和文件初始化器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class VaultDirectoryInitializer {

    /** 默认目录列表 */
    private static final String[] DEFAULT_DIRECTORIES = {
            "raw/sources",
            "raw/assets",
            "wiki/sources",
            "wiki/entities",
            "wiki/concepts",
            "wiki/questions",
            "wiki/synthesis",
            ".ai-wiki/cache",
            ".ai-wiki/history"
    };

    /** Vault 文件访问服务 */
    private final VaultFileService vaultFileService;

    /**
     * 创建 Vault 目录初始化器。
     *
     * @param vaultFileService Vault 文件访问服务
     */
    public VaultDirectoryInitializer(VaultFileService vaultFileService) {
        this.vaultFileService = vaultFileService;
    }

    /**
     * 初始化缺失目录和默认文件。
     *
     * @param vaultRoot Vault 根目录
     * @param purpose 知识库目标摘要
     */
    public void initialize(Path vaultRoot, String purpose) {
        for (String directory : DEFAULT_DIRECTORIES) {
            vaultFileService.createDirectories(vaultRoot, directory);
        }
        vaultFileService.writeStringIfAbsent(vaultRoot, "purpose.md", defaultPurpose(purpose));
        vaultFileService.writeStringIfAbsent(vaultRoot, "schema.md", defaultSchema());
        vaultFileService.writeStringIfAbsent(vaultRoot, "wiki/index.md", "# Index\n\n");
        vaultFileService.writeStringIfAbsent(vaultRoot, "wiki/log.md", "# Log\n\n");
        vaultFileService.writeStringIfAbsent(vaultRoot, "wiki/overview.md", "# Overview\n\n");
    }

    /**
     * 创建默认 purpose.md 内容。
     *
     * @param purpose 用户传入目标
     * @return 默认内容
     */
    private String defaultPurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "# Purpose\n\n";
        }
        return "# Purpose\n\n" + purpose.strip() + "\n";
    }

    /**
     * 创建默认 schema.md 内容。
     *
     * @return 默认内容
     */
    private String defaultSchema() {
        return "# Schema\n\n"
                + "- sources: source notes generated from imported materials\n"
                + "- entities: people, organizations, products, and places\n"
                + "- concepts: reusable ideas and terminology\n"
                + "- questions: saved question answers\n"
                + "- synthesis: synthesized wiki pages\n";
    }
}
