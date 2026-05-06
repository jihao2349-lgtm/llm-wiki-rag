package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.dto.vault.VaultInitDTO;
import com.jihao.aiwiki.dto.vault.VaultReindexDTO;
import com.jihao.aiwiki.service.VaultService;
import com.jihao.aiwiki.vo.vault.VaultDetailVO;
import com.jihao.aiwiki.vo.vault.VaultReindexVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vault API 控制器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Validated
@RestController
@RequestMapping("/api/vault")
@Tag(name = "Vault")
public class VaultController {

    /** Vault 项目生命周期服务提供器 */
    private final ObjectProvider<VaultService> vaultServiceProvider;

    /**
     * 创建 Vault 控制器。
     *
     * @param vaultServiceProvider Vault 项目生命周期服务提供器
     */
    public VaultController(ObjectProvider<VaultService> vaultServiceProvider) {
        this.vaultServiceProvider = vaultServiceProvider;
    }

    /**
     * 初始化或绑定 Vault。
     *
     * @param initDTO 初始化请求
     * @return Vault 详情
     */
    @PostMapping("/init")
    @Operation(summary = "初始化或绑定 Vault")
    public ApiResponse<VaultDetailVO> init(@Valid @RequestBody VaultInitDTO initDTO) {
        return ApiResponse.success(vaultService().initVault(initDTO));
    }

    /**
     * 获取 Vault 详情。
     *
     * @param vaultId Vault ID，为空时返回最近绑定的 Vault
     * @return Vault 详情
     */
    @GetMapping("/detail")
    @Operation(summary = "获取 Vault 详情")
    public ApiResponse<VaultDetailVO> detail(@Positive @RequestParam(required = false) Long vaultId) {
        return ApiResponse.success(vaultService().getDetail(vaultId));
    }

    /**
     * 创建重建索引占位结果。
     *
     * @param reindexDTO 重建请求
     * @return 重建占位结果
     */
    @PostMapping("/reindex")
    @Operation(summary = "重建 Vault 索引")
    public ApiResponse<VaultReindexVO> reindex(@Valid @RequestBody VaultReindexDTO reindexDTO) {
        return ApiResponse.success(vaultService().reindex(reindexDTO));
    }

    /**
     * 获取 Vault 服务。
     *
     * @return Vault 服务
     */
    private VaultService vaultService() {
        return vaultServiceProvider.getObject();
    }
}
