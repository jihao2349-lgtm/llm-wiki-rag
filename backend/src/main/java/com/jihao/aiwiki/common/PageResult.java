package com.jihao.aiwiki.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应体。
 *
 * @param <T> 分页记录类型
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应体")
public class PageResult<T> {

    /** 当前页记录 */
    @Schema(description = "当前页记录")
    private List<T> records;

    /** 总记录数 */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /** 页码，从 1 开始 */
    @Schema(description = "页码，从 1 开始", example = "1")
    private Long pageNo;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "20")
    private Long pageSize;
}
