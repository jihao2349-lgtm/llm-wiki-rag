package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.VaultProjectDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Vault 项目数据库访问。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface VaultProjectMapper {

    /**
     * 根据主键查询未删除 Vault。
     *
     * @param id Vault ID
     * @return Vault 项目
     */
    @Select("""
            SELECT id, name, path, purpose, status, last_indexed_at, deleted, create_time, update_time
            FROM vault_project
            WHERE id = #{id} AND deleted = 0
            """)
    VaultProjectDO selectById(@Param("id") Long id);

    /**
     * 根据路径查询未删除 Vault。
     *
     * @param path Vault 规范化绝对路径
     * @return Vault 项目
     */
    @Select("""
            SELECT id, name, path, purpose, status, last_indexed_at, deleted, create_time, update_time
            FROM vault_project
            WHERE path = #{path} AND deleted = 0
            LIMIT 1
            """)
    VaultProjectDO selectByPath(@Param("path") String path);

    /**
     * 查询最近绑定的未删除 Vault。
     *
     * @return Vault 项目
     */
    @Select("""
            SELECT id, name, path, purpose, status, last_indexed_at, deleted, create_time, update_time
            FROM vault_project
            WHERE deleted = 0
            ORDER BY update_time DESC, id DESC
            LIMIT 1
            """)
    VaultProjectDO selectLatest();

    /**
     * 插入 Vault 项目。
     *
     * @param vaultProject Vault 项目
     * @return 影响行数
     */
    @Insert("""
            INSERT INTO vault_project (name, path, purpose, status, deleted)
            VALUES (#{name}, #{path}, #{purpose}, #{status}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VaultProjectDO vaultProject);

    /**
     * 更新已存在 Vault 的基础信息。
     *
     * @param vaultProject Vault 项目
     * @return 影响行数
     */
    @Update("""
            UPDATE vault_project
            SET name = #{name}, purpose = #{purpose}, status = #{status}
            WHERE id = #{id} AND deleted = 0
            """)
    int updateBinding(VaultProjectDO vaultProject);
}
