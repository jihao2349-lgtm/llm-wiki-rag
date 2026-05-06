package com.jihao.aiwiki.domain.search;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 解析后的 Markdown Frontmatter 结构。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
public class ParsedFrontmatter {

    /** 页面标题，来自 frontmatter title 字段 */
    private String title;

    /** 页面类型，来自 frontmatter type 字段 */
    private String type;

    /** 标签列表 */
    private List<String> tags;

    /** 关联页面列表 */
    private List<String> related;

    /** 最后更新日期字符串 */
    private String updated;

    /** Frontmatter 之后的正文内容 */
    private String body;
}
