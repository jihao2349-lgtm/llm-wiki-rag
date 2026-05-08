package com.jihao.aiwiki.vo.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 向量化失败页面详情。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Builder
@Schema(description = "向量化失败页面")
public class EmbeddingFailedPageVO {

    @Schema(description = "页面 ID")
    private Long pageId;

    @Schema(description = "页面路径")
    private String path;

    @Schema(description = "页面标题")
    private String title;

    @Schema(description = "失败原因")
    private String error;
}
