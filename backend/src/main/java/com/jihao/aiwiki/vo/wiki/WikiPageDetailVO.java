package com.jihao.aiwiki.vo.wiki;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wiki 页面详情 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPageDetailVO {

    /** Wiki 相对路径 */
    private String path;

    /** 页面标题 */
    private String title;

    /** 页面类型 */
    private String type;

    /** 标签列表 */
    private List<String> tags;

    /** 关联页面路径列表 */
    private List<String> related;

    /** Markdown 全文 */
    private String content;
}
