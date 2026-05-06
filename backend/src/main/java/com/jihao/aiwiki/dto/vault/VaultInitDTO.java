package com.jihao.aiwiki.dto.vault;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Vault 初始化请求。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "Vault 初始化请求")
public class VaultInitDTO {

    /** Vault 本地绝对路径 */
    @NotBlank
    @Size(max = 1024)
    @Schema(description = "Vault 本地绝对路径", example = "/Users/me/Obsidian/Vault")
    private String path;

    /** Vault 名称，不传时使用目录名 */
    @Size(max = 128)
    @Schema(description = "Vault 名称，不传时使用目录名", example = "Research Vault")
    private String name;

    /** 知识库目标摘要 */
    @Size(max = 1024)
    @Schema(description = "知识库目标摘要", example = "整理 AI Agent 研究资料")
    private String purpose;
}
