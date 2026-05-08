package com.jihao.aiwiki.domain.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带评分的搜索结果页面，兼容关键词检索与向量检索。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredPage {

    /** 页面主键 ID（向量检索时由 mapper 填充） */
    private Long pageId;

    /** Wiki 相对路径 */
    private String path;

    /** 页面标题 */
    private String title;

    /** 匹配分数，越高越相关（关键词检索用） */
    private int score;

    /** 摘要片段，包含匹配关键词的上下文 */
    private String snippet;

    /** 正文内容 */
    private String body;

    /** 页面类型 */
    private String type;

    /** 向量距离（越小越相似，向量检索时填充） */
    private double vectorDistance;

    /** 关键词检索专用构造（兼容原有调用方） */
    public ScoredPage(String path, String title, int score, String snippet, String body, String type) {
        this.path = path;
        this.title = title;
        this.score = score;
        this.snippet = snippet;
        this.body = body;
        this.type = type;
    }
}
