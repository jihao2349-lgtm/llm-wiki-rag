package com.jihao.aiwiki.dto.setting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * LLM 连通性测试请求。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Schema(description = "LLM 连通性测试请求")
public class SettingTestRequest {

    /** Provider 标识，空值表示使用已保存设置 */
    @Schema(description = "Provider 标识")
    private String provider;

    /** API Base URL，空值表示使用已保存设置 */
    @Schema(description = "API Base URL")
    private String baseUrl;

    /** API Key，空值表示使用已保存密钥 */
    @Schema(description = "API Key")
    private String apiKey;

    /** 模型名，空值表示使用已保存设置 */
    @Schema(description = "模型名称")
    private String model;

    /** 测试温度，空值使用默认值 */
    @Schema(description = "测试温度")
    private BigDecimal temperature;
}
