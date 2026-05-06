package com.jihao.aiwiki.vo.wiki;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wiki 关键词搜索结果 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiSearchResultVO {

    /** Wiki 相对路径 */
    private String path;

    /** 页面标题 */
    private String title;

    /** 页面类型 */
    private String type;

    /** 相关分数 */
    private int score;

    /** 含关键词的摘要片段 */
    private String snippet;
}
