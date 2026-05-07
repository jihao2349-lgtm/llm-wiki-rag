package com.jihao.aiwiki.dto.wiki;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 在 Obsidian 中打开 Wiki 页面请求。
 *
 * @author jihao
 * @date 2026/05/07
 */
@Data
@Schema(description = "打开 Wiki 页面请求")
public class OpenWikiDTO {

    @Schema(description = "Vault ID")
    private Long vaultId;

    @Schema(description = "Wiki 页面相对路径", example = "wiki/concepts/AI.md")
    private String path;
}
