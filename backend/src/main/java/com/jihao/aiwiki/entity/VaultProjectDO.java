package com.jihao.aiwiki.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Vault 项目表数据对象。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class VaultProjectDO {

    /** 主键 ID */
    private Long id;

    /** Vault 名称 */
    private String name;

    /** Vault 绝对路径 */
    private String path;

    /** 知识库目标摘要 */
    private String purpose;

    /** 状态 */
    private String status;

    /** 最近索引时间 */
    private LocalDateTime lastIndexedAt;

    /** 逻辑删除标记 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
