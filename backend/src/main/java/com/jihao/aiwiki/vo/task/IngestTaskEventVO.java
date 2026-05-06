package com.jihao.aiwiki.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 摄入任务 SSE 事件。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "摄入任务 SSE 事件")
public class IngestTaskEventVO {

    /** 事件类型 */
    @Schema(description = "事件类型")
    private String type;

    /** 任务数据 */
    @Schema(description = "任务数据")
    private IngestTaskVO task;
}
