package com.jihao.aiwiki.vo.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * LLM 连通性测试结果。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@Schema(description = "LLM 连通性测试结果")
public class SettingTestVO {

    /** 测试是否成功 */
    @Schema(description = "测试是否成功")
    private Boolean success;

    /** 脱敏后的测试消息 */
    @Schema(description = "脱敏后的测试消息")
    private String message;

    /** 实际测试的模型 */
    @Schema(description = "实际测试的模型")
    private String model;
}
