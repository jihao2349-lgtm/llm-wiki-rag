package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.SourceDocumentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 原始资料 MyBatis Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface SourceDocumentMapper {

    /**
     * 插入资料记录，回写生成主键。
     *
     * @param doc 资料实体
     */
    void insert(SourceDocumentDO doc);

    /**
     * 按主键查询（逻辑未删除）。
     *
     * @param id 主键
     * @return 资料实体，不存在时返回 null
     */
    SourceDocumentDO selectById(Long id);

    /**
     * 按 Vault 和路径查询（逻辑未删除）。
     *
     * @param vaultId      Vault ID
     * @param originalPath 原始文件相对路径
     * @return 资料实体，不存在时返回 null
     */
    SourceDocumentDO selectByVaultIdAndPath(@Param("vaultId") Long vaultId,
                                             @Param("originalPath") String originalPath);

    /**
     * 分页查询资料列表。
     *
     * @param vaultId  Vault ID
     * @param type     类型过滤，null 时不过滤
     * @param status   状态过滤，null 时不过滤
     * @param offset   偏移量
     * @param limit    每页数量
     * @return 资料列表
     */
    List<SourceDocumentDO> selectPage(@Param("vaultId") Long vaultId,
                                       @Param("type") String type,
                                       @Param("status") String status,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 分页查询总数。
     *
     * @param vaultId Vault ID
     * @param type    类型过滤，null 时不过滤
     * @param status  状态过滤，null 时不过滤
     * @return 总数
     */
    long countPage(@Param("vaultId") Long vaultId,
                   @Param("type") String type,
                   @Param("status") String status);

    /**
     * 更新资料记录所有字段。
     *
     * @param doc 资料实体
     */
    void updateByPrimaryKey(SourceDocumentDO doc);

    /**
     * 按 Vault 统计资料数量。
     *
     * @param vaultId Vault ID
     * @return 资料总数
     */
    long countByVaultId(@Param("vaultId") Long vaultId);
}
