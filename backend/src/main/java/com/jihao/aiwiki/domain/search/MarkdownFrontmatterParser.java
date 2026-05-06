package com.jihao.aiwiki.domain.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown Frontmatter 解析器。
 * 采用简单行扫描，无外部 YAML 库依赖，适合 v0.1 内嵌格式。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class MarkdownFrontmatterParser {

    /**
     * 解析 Markdown 字符串，提取 frontmatter 字段和正文。
     *
     * @param content Markdown 全文
     * @return 解析结果
     */
    public ParsedFrontmatter parse(String content) {
        if (content == null) {
            content = "";
        }
        ParsedFrontmatter.ParsedFrontmatterBuilder builder = ParsedFrontmatter.builder()
                .tags(List.of()).related(List.of());

        if (!content.startsWith("---")) {
            return builder.body(content).build();
        }

        String[] lines = content.split("\n", -1);
        int endIdx = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) {
                endIdx = i;
                break;
            }
        }
        if (endIdx < 0) {
            return builder.body(content).build();
        }

        String title = null, type = null, updated = null;
        List<String> tags = new ArrayList<>(), related = new ArrayList<>();
        boolean inTags = false, inRelated = false;

        for (int i = 1; i < endIdx; i++) {
            String line = lines[i];
            if (line.startsWith("title:")) {
                title = line.substring(6).trim();
                inTags = inRelated = false;
            } else if (line.startsWith("type:")) {
                type = line.substring(5).trim();
                inTags = inRelated = false;
            } else if (line.startsWith("updated:")) {
                updated = line.substring(8).trim();
                inTags = inRelated = false;
            } else if (line.startsWith("tags:")) {
                String v = line.substring(5).trim();
                inTags = true;
                inRelated = false;
                if (!v.isEmpty() && !v.equals("[]")) {
                    parseSingleLineList(v, tags);
                }
            } else if (line.startsWith("related:")) {
                String v = line.substring(8).trim();
                inRelated = true;
                inTags = false;
                if (!v.isEmpty() && !v.equals("[]")) {
                    parseSingleLineList(v, related);
                }
            } else if (line.startsWith("  - ") || line.startsWith("- ")) {
                String item = line.replaceFirst("^\\s*- ", "").trim();
                if (!item.isEmpty()) {
                    if (inTags) tags.add(item);
                    else if (inRelated) related.add(item);
                }
            } else {
                inTags = inRelated = false;
            }
        }

        StringBuilder bodyBuilder = new StringBuilder();
        for (int i = endIdx + 1; i < lines.length; i++) {
            bodyBuilder.append(lines[i]);
            if (i < lines.length - 1) {
                bodyBuilder.append('\n');
            }
        }

        return builder
                .title(title).type(type).updated(updated)
                .tags(tags).related(related)
                .body(bodyBuilder.toString().stripLeading())
                .build();
    }

    /**
     * 解析单行内联列表，如 {@code [a, b, c]} 或 {@code a, b, c}。
     *
     * @param value  行值
     * @param target 目标列表
     */
    private void parseSingleLineList(String value, List<String> target) {
        value = value.replaceAll("^\\[|]$", "");
        for (String s : value.split(",")) {
            String t = s.trim().replaceAll("^['\"]|['\"]$", "");
            if (!t.isEmpty()) {
                target.add(t);
            }
        }
    }
}
