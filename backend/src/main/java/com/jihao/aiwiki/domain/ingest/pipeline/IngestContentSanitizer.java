package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 输出内容清洗器，按顺序执行三条规则：
 * 1. 剥除外层 code fence（```yaml / ```markdown / ```）
 * 2. 去除 frontmatter: 前缀行
 * 3. 修复 frontmatter 内 wikilink 列表格式
 */
@Component
public class IngestContentSanitizer {

    // 匹配 frontmatter 内的 related 字段（含 wikilink）
    private static final Pattern RELATED_WIKILINK =
            Pattern.compile("^(related:\\s*)(\\[\\[.+)$", Pattern.MULTILINE);
    // 提取单个 [[...]] token
    private static final Pattern WIKILINK_TOKEN = Pattern.compile("\\[\\[[^]]+]]");

    public String sanitize(String content) {
        if (content == null) return null;
        content = stripOuterFence(content);
        content = stripFrontmatterPrefix(content);
        content = fixWikilinkList(content);
        return content;
    }

    // 规则 1：剥除外层 code fence
    private String stripOuterFence(String content) {
        String stripped = content.stripLeading();
        if (!stripped.startsWith("```")) return content;

        // 找到第一个换行（跳过 ```yaml / ```markdown / ``` 行）
        int firstNewline = stripped.indexOf('\n');
        if (firstNewline < 0) return content;

        String body = stripped.substring(firstNewline + 1);

        // 去掉末尾的 ``` 行
        int lastFence = body.lastIndexOf("\n```");
        if (lastFence >= 0) {
            body = body.substring(0, lastFence);
        } else if (body.stripTrailing().endsWith("```")) {
            body = body.substring(0, body.stripTrailing().lastIndexOf("```"));
        }
        return body;
    }

    // 规则 2：去除 frontmatter: 前缀行
    private String stripFrontmatterPrefix(String content) {
        String stripped = content.stripLeading();
        if (stripped.toLowerCase().startsWith("frontmatter:")) {
            int newline = stripped.indexOf('\n');
            if (newline >= 0) {
                return stripped.substring(newline + 1);
            }
        }
        return content;
    }

    // 规则 3：修复 frontmatter 内的 wikilink 列表（仅作用于 frontmatter 区域）
    private String fixWikilinkList(String content) {
        // 定位 frontmatter 区域（第一个 --- 到第二个 ---）
        if (!content.stripLeading().startsWith("---")) return content;

        int firstDash = content.indexOf("---");
        int secondDash = content.indexOf("---", firstDash + 3);
        if (secondDash < 0) return content;

        String frontmatter = content.substring(firstDash, secondDash + 3);
        String rest = content.substring(secondDash + 3);

        Matcher matcher = RELATED_WIKILINK.matcher(frontmatter);
        if (!matcher.find()) return content;

        StringBuffer sb = new StringBuffer();
        matcher.reset();
        while (matcher.find()) {
            String key = matcher.group(1);     // "related: "
            String value = matcher.group(2);   // "[[a]], [[b]], [[c]]"

            List<String> tokens = new ArrayList<>();
            Matcher tokenMatcher = WIKILINK_TOKEN.matcher(value);
            while (tokenMatcher.find()) {
                tokens.add("\"" + tokenMatcher.group() + "\"");
            }
            String fixed = key + "[" + String.join(", ", tokens) + "]";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(fixed));
        }
        matcher.appendTail(sb);

        return sb + rest;
    }
}
