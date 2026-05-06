package com.jihao.aiwiki.dto.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 导入网页 URL 请求 DTO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class SourceUrlImportDTO {

    @NotNull
    private Long vaultId;

    @NotBlank
    private String url;
}
