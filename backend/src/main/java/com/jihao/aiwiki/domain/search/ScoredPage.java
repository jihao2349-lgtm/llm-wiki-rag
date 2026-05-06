package com.jihao.aiwiki.domain.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 带评分的搜索结果页面，用于关键词检索排序。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoredPage {

    /** Wiki 相对路径 */
    private String path;

    /** 页面标题 */
    private String title;

    /** 匹配分数，越高越相关 */
    private int score;

    /** 摘要片段，包含匹配关键词的上下文 */
    private String snippet;

    /** 正文内容 */
    private String body;

    /** 页面类型 */
    private String type;
}
