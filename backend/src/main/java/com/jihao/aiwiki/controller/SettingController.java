package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.dto.setting.SettingTestRequest;
import com.jihao.aiwiki.dto.setting.SettingUpdateRequest;
import com.jihao.aiwiki.service.SettingService;
import com.jihao.aiwiki.vo.setting.SettingDetailVO;
import com.jihao.aiwiki.vo.setting.SettingTestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Settings API controller.
 *
 * @author jihao
 * @date 2026/05/06
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
@Tag(name = "Settings", description = "LLM 设置接口")
public class SettingController {

    /** 应用配置服务 */
    private final SettingService settingService;

    /**
     * 获取当前 LLM 设置。
     *
     * @return 当前 LLM 设置
     */
    @GetMapping("/detail")
    @Operation(summary = "获取设置")
    public ApiResponse<SettingDetailVO> detail() {
        return ApiResponse.success(settingService.getDetail());
    }

    /**
     * 更新 LLM 设置。
     *
     * @param request 设置更新请求
     * @return 更新后的 LLM 设置
     */
    @PutMapping("/update")
    @Operation(summary = "更新模型和索引设置")
    public ApiResponse<SettingDetailVO> update(@Valid @RequestBody SettingUpdateRequest request) {
        return ApiResponse.success(settingService.update(request));
    }

    /**
     * 测试 LLM 连通性。
     *
     * @param request 连通性测试请求
     * @return 连通性测试结果
     */
    @PostMapping("/test-llm")
    @Operation(summary = "测试 LLM 连通性")
    public ApiResponse<SettingTestVO> testLlm(@RequestBody(required = false) SettingTestRequest request) {
        return ApiResponse.success(settingService.testLlm(request == null ? new SettingTestRequest() : request));
    }
}
