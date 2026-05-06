package com.jihao.aiwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始资料数据库实体，映射 source_document 表。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocumentDO {

    /** 主键 ID */
    private Long id;

    /** Vault ID */
    private Long vaultId;

    /** 资料类型：FILE / URL */
    private String type;

    /** 资料标题 */
    private String title;

    /** 原始文件相对路径 */
    private String originalPath;

    /** 原始文件 SHA-256 哈希 */
    private String originalHash;

    /** 解析文本相对路径 */
    private String extractedTextPath;

    /** 网页原始 URL */
    private String sourceUrl;

    /** 状态：PENDING / PARSED / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 逻辑删除标志 */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
