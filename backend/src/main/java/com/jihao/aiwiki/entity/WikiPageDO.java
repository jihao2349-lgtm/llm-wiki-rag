package com.jihao.aiwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wiki 页面索引数据库实体，映射 wiki_page 表。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageDO {

    /** 主键 ID */
    private Long id;

    /** Vault ID */
    private Long vaultId;

    /** Wiki 相对路径 */
    private String path;

    /** 页面标题 */
    private String title;

    /** 页面类型 */
    private String type;

    /** 标签 JSON 字符串 */
    private String tags;

    /** 关联页面 JSON 字符串 */
    private String related;

    /** 内容哈希 */
    private String contentHash;

    /** 向量化状态：PENDING / SUCCESS / FAILED */
    private String embedStatus;

    /** 生成向量所用模型 */
    private String embeddingModel;

    /** 向量生成时间 */
    private LocalDateTime embeddedAt;

    /** 向量化时内容的 sha256 hash，用于增量判断 */
    private String embedContentHash;

    /** 向量化失败原因 */
    private String embedError;

    /** 逻辑删除标志 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
