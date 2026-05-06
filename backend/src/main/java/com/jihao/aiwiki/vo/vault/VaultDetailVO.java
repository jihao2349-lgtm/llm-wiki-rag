package com.jihao.aiwiki.vo.vault;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Vault 详情视图对象。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@Schema(description = "Vault 详情")
public class VaultDetailVO {

    /** Vault ID */
    @Schema(description = "Vault ID", example = "1")
    private Long id;

    /** Vault 名称 */
    @Schema(description = "Vault 名称", example = "Research Vault")
    private String name;

    /** Vault 绝对路径 */
    @Schema(description = "Vault 绝对路径", example = "/Users/me/Obsidian/Vault")
    private String path;

    /** 知识库目标摘要 */
    @Schema(description = "知识库目标摘要")
    private String purpose;

    /** Vault 状态 */
    @Schema(description = "Vault 状态", example = "READY")
    private String status;

    /** 最近索引时间 */
    @Schema(description = "最近索引时间")
    private LocalDateTime lastIndexedAt;
}
