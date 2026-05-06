package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.WikiPageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Wiki 页面索引 MyBatis Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface WikiPageMapper {

    /**
     * 插入页面记录，回写生成主键。
     *
     * @param page 页面实体
     */
    void insert(WikiPageDO page);

    /**
     * 按主键查询（逻辑未删除）。
     *
     * @param id 主键
     * @return 页面实体，不存在时返回 null
     */
    WikiPageDO selectById(Long id);

    /**
     * 按 Vault 和路径查询（逻辑未删除）。
     *
     * @param vaultId Vault ID
     * @param path    Wiki 相对路径
     * @return 页面实体，不存在时返回 null
     */
    WikiPageDO selectByVaultIdAndPath(@Param("vaultId") Long vaultId, @Param("path") String path);

    /**
     * 查询 Vault 所有未删除页面。
     *
     * @param vaultId Vault ID
     * @return 页面列表
     */
    List<WikiPageDO> selectByVaultId(@Param("vaultId") Long vaultId);

    /**
     * 按 Vault 统计页面数量（逻辑未删除）。
     *
     * @param vaultId Vault ID
     * @return 页面数量
     */
    long countByVaultId(@Param("vaultId") Long vaultId);

    /**
     * INSERT ON DUPLICATE KEY UPDATE，按 vault_id + path 唯一键幂等写入。
     *
     * @param page 页面实体
     */
    void upsert(WikiPageDO page);

    /**
     * 将不在 activePaths 列表中的页面标记为逻辑删除。
     *
     * @param vaultId     Vault ID
     * @param activePaths 当前存活页面路径列表
     */
    void markDeletedByVaultIdAndPathNotIn(@Param("vaultId") Long vaultId,
                                          @Param("activePaths") List<String> activePaths);
}
