package com.jihao.aiwiki.domain.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Markdown 文本中提取 [[wikilink]] 链接目标。
 * 支持 {@code [[link]]} 和 {@code [[link|alias]]} 两种格式。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class WikilinkExtractor {

    /** 匹配 [[link]] 和 [[link|alias]] 格式的正则表达式 */
    private static final Pattern WIKILINK_PATTERN =
            Pattern.compile("\\[\\[([^\\]|]+)(?:\\|[^\\]]*)?]]");

    /**
     * 从 Markdown 文本中提取所有 wikilink 链接目标（不去重）。
     *
     * @param content Markdown 文本
     * @return 链接目标列表
     */
    public List<String> extract(String content) {
        List<String> links = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return links;
        }
        Matcher matcher = WIKILINK_PATTERN.matcher(content);
        while (matcher.find()) {
            links.add(matcher.group(1).trim());
        }
        return links;
    }
}
