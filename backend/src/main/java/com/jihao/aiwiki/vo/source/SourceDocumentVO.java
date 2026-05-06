package com.jihao.aiwiki.vo.source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 原始资料展示 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceDocumentVO {

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

    /** 网页原始 URL */
    private String sourceUrl;

    /** 状态：PENDING / PARSED / FAILED */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createTime;
}
