package com.jihao.aiwiki.service;

import com.jihao.aiwiki.dto.setting.SettingTestRequest;
import com.jihao.aiwiki.dto.setting.SettingUpdateRequest;
import com.jihao.aiwiki.vo.setting.SettingDetailVO;
import com.jihao.aiwiki.vo.setting.SettingTestVO;

/**
 * 应用配置服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface SettingService {

    /**
     * 获取当前 LLM 设置。
     *
     * @return LLM 设置详情
     */
    SettingDetailVO getDetail();

    /**
     * 更新当前 LLM 设置。
     *
     * @param request 设置更新请求
     * @return 更新后的 LLM 设置详情
     */
    SettingDetailVO update(SettingUpdateRequest request);

    /**
     * 测试 LLM 连通性。
     *
     * @param request 连通性测试请求
     * @return 连通性测试结果
     */
    SettingTestVO testLlm(SettingTestRequest request);
}
