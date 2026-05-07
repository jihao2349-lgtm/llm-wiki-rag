package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * frontmatter sources 字段合并器。
 * 支持 inline 格式 ["a","b"] 和 multi-line 格式（- item），合并后统一输出 inline 格式。
 */
@Component
public class SourcesMerger {

    // inline: sources: ["a.pdf", "b.pdf"]
    private static final Pattern INLINE_SOURCES =
            Pattern.compile("sources:\\s*\\[([^]]*)]", Pattern.MULTILINE);
    // multi-line: sources:\n  - a.pdf
    private static final Pattern MULTILINE_SOURCES =
            Pattern.compile("sources:\\s*\\n((?:[ \\t]*-[ \\t]+.+\\n?)+)", Pattern.MULTILINE);
    // single quoted/unquoted item inside inline array
    private static final Pattern INLINE_ITEM =
            Pattern.compile("\"([^\"]+)\"|'([^']+)'|([^,\"'\\[\\]\\s][^,\"'\\[\\]]*[^,\"'\\[\\]\\s]|[^,\"'\\[\\]\\s]+)");
    // multi-line list item
    private static final Pattern MULTILINE_ITEM =
            Pattern.compile("[ \\t]*-[ \\t]+(.+)");
    // sources 行（用于替换）
    private static final Pattern SOURCES_LINE =
            Pattern.compile("sources:\\s*(?:\\[[^]]*]|\\n(?:[ \\t]*-[ \\t]+.+\\n?)+|[^\\n]*)");

    /**
     * 从 wiki 页面内容中解析 sources 列表。
     */
    public List<String> parseSources(String content) {
        if (content == null || content.isBlank()) return List.of();

        // 优先尝试 inline 格式
        Matcher inline = INLINE_SOURCES.matcher(content);
        if (inline.find()) {
            return parseInlineItems(inline.group(1));
        }

        // 再尝试 multi-line 格式
        Matcher multi = MULTILINE_SOURCES.matcher(content);
        if (multi.find()) {
            return parseMultilineItems(multi.group(1));
        }

        return List.of();
    }

    /**
     * 合并两个 sources 列表，大小写不敏感去重，existing 顺序优先，incoming 新条目追加。
     */
    public List<String> mergeSourcesLists(List<String> existing, List<String> incoming) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();

        for (String item : existing) {
            String key = item.trim().toLowerCase();
            if (seen.add(key)) result.add(item.trim());
        }
        for (String item : incoming) {
            String key = item.trim().toLowerCase();
            if (seen.add(key)) result.add(item.trim());
        }
        return result;
    }

    /**
     * 将 content 中的 sources 字段替换为给定列表，统一输出 inline 格式。
     */
    public String writeSources(String content, List<String> sources) {
        String inlineValue = buildInline(sources);
        Matcher matcher = SOURCES_LINE.matcher(content);
        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement("sources: " + inlineValue));
        }
        return content;
    }

    /**
     * 主入口：将 newContent 的 sources 与 existingContent 的 sources 合并后写回 newContent。
     * existingContent 为 null 或空时直接返回 newContent。
     */
    public String mergeSourcesIntoContent(String newContent, String existingContent) {
        if (existingContent == null || existingContent.isBlank()) return newContent;

        List<String> existingSources = parseSources(existingContent);
        List<String> newSources = parseSources(newContent);
        List<String> merged = mergeSourcesLists(existingSources, newSources);
        return writeSources(newContent, merged);
    }

    // ---- private helpers ----

    private List<String> parseInlineItems(String arrayBody) {
        List<String> result = new ArrayList<>();
        Matcher m = INLINE_ITEM.matcher(arrayBody);
        while (m.find()) {
            String item = m.group(1) != null ? m.group(1)
                    : m.group(2) != null ? m.group(2)
                    : m.group(3);
            if (item != null && !item.isBlank()) result.add(item.trim());
        }
        return result;
    }

    private List<String> parseMultilineItems(String block) {
        List<String> result = new ArrayList<>();
        for (String line : block.split("\n")) {
            Matcher m = MULTILINE_ITEM.matcher(line);
            if (m.matches()) {
                String item = m.group(1).trim().replaceAll("^[\"']|[\"']$", "");
                if (!item.isBlank()) result.add(item);
            }
        }
        return result;
    }

    private String buildInline(List<String> sources) {
        if (sources.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(sources.get(i)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }
}
