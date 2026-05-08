package com.jihao.aiwiki.vo.embedding;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 向量化任务进度响应。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Data
@Builder
@Schema(description = "向量化任务进度")
public class EmbeddingProgressVO {

    @Schema(description = "是否正在处理")
    private boolean processing;

    @Schema(description = "已处理数量")
    private int current;

    @Schema(description = "总数量")
    private int total;
}
