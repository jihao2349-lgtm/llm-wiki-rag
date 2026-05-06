package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.domain.vault.VaultDirectoryInitializer;
import com.jihao.aiwiki.domain.vault.VaultPathValidator;
import com.jihao.aiwiki.dto.vault.VaultInitDTO;
import com.jihao.aiwiki.dto.vault.VaultReindexDTO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.service.VaultService;
import com.jihao.aiwiki.vo.vault.VaultDetailVO;
import com.jihao.aiwiki.vo.vault.VaultReindexVO;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vault 项目生命周期服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
@ConditionalOnBean(VaultProjectMapper.class)
public class VaultServiceImpl implements VaultService {

    /** Vault 就绪状态 */
    private static final String STATUS_READY = "READY";

    /** Vault 项目数据库访问 */
    private final VaultProjectMapper vaultProjectMapper;

    /** Vault 路径校验器 */
    private final VaultPathValidator pathValidator;

    /** Vault 目录初始化器 */
    private final VaultDirectoryInitializer directoryInitializer;

    /**
     * 创建 Vault 服务。
     *
     * @param vaultProjectMapper Vault 项目数据库访问
     * @param pathValidator Vault 路径校验器
     * @param directoryInitializer Vault 目录初始化器
     */
    public VaultServiceImpl(
            VaultProjectMapper vaultProjectMapper,
            VaultPathValidator pathValidator,
            VaultDirectoryInitializer directoryInitializer) {
        this.vaultProjectMapper = vaultProjectMapper;
        this.pathValidator = pathValidator;
        this.directoryInitializer = directoryInitializer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VaultDetailVO initVault(VaultInitDTO initDTO) {
        Path realVaultPath = pathValidator.validateVaultRoot(Path.of(initDTO.getPath()));
        directoryInitializer.initialize(realVaultPath, initDTO.getPurpose());

        VaultProjectDO vaultProject = vaultProjectMapper.selectByPath(realVaultPath.toString());
        if (vaultProject == null) {
            vaultProject = new VaultProjectDO();
            vaultProject.setName(resolveVaultName(initDTO, realVaultPath));
            vaultProject.setPath(realVaultPath.toString());
            vaultProject.setPurpose(initDTO.getPurpose());
            vaultProject.setStatus(STATUS_READY);
            vaultProjectMapper.insert(vaultProject);
            return toDetailVO(vaultProject);
        }

        vaultProject.setName(resolveVaultName(initDTO, realVaultPath));
        vaultProject.setPurpose(initDTO.getPurpose());
        vaultProject.setStatus(STATUS_READY);
        vaultProjectMapper.updateBinding(vaultProject);
        return toDetailVO(vaultProject);
    }

    @Override
    public VaultDetailVO getDetail(Long vaultId) {
        VaultProjectDO vaultProject = vaultId == null
                ? vaultProjectMapper.selectLatest()
                : vaultProjectMapper.selectById(vaultId);
        if (vaultProject == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED);
        }
        return toDetailVO(vaultProject);
    }

    @Override
    public VaultReindexVO reindex(VaultReindexDTO reindexDTO) {
        VaultDetailVO detail = getDetail(reindexDTO.getVaultId());
        return VaultReindexVO.builder()
                .vaultId(detail.getId())
                .status("ACCEPTED")
                .message("reindex placeholder accepted; wiki index rebuild is owned by T5")
                .build();
    }

    /**
     * 解析 Vault 名称。
     *
     * @param initDTO 初始化请求
     * @param realVaultPath Vault 真实路径
     * @return Vault 名称
     */
    private String resolveVaultName(VaultInitDTO initDTO, Path realVaultPath) {
        if (initDTO.getName() != null && !initDTO.getName().isBlank()) {
            return initDTO.getName().strip();
        }
        Path fileName = realVaultPath.getFileName();
        return fileName == null ? realVaultPath.toString() : fileName.toString();
    }

    /**
     * 转换为 Vault 详情 VO。
     *
     * @param vaultProject Vault 项目
     * @return Vault 详情
     */
    private VaultDetailVO toDetailVO(VaultProjectDO vaultProject) {
        return VaultDetailVO.builder()
                .id(vaultProject.getId())
                .name(vaultProject.getName())
                .path(vaultProject.getPath())
                .purpose(vaultProject.getPurpose())
                .status(vaultProject.getStatus())
                .lastIndexedAt(vaultProject.getLastIndexedAt())
                .build();
    }
}
