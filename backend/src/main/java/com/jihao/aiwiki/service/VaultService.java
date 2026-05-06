package com.jihao.aiwiki.service;

import com.jihao.aiwiki.dto.vault.VaultInitDTO;
import com.jihao.aiwiki.dto.vault.VaultReindexDTO;
import com.jihao.aiwiki.vo.vault.VaultDetailVO;
import com.jihao.aiwiki.vo.vault.VaultReindexVO;

/**
 * Vault 项目生命周期服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface VaultService {

    /**
     * 初始化或绑定本地 Obsidian Vault。
     *
     * @param initDTO 初始化请求
     * @return Vault 详情
     */
    VaultDetailVO initVault(VaultInitDTO initDTO);

    /**
     * 获取 Vault 详情。
     *
     * @param vaultId Vault ID，为空时返回最近绑定的 Vault
     * @return Vault 详情
     */
    VaultDetailVO getDetail(Long vaultId);

    /**
     * 创建重建索引占位结果。
     *
     * @param reindexDTO 重建请求
     * @return 重建占位结果
     */
    VaultReindexVO reindex(VaultReindexDTO reindexDTO);
}
