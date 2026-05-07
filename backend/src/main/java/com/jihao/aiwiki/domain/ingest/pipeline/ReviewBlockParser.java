package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 输出 REVIEW block 解析器。
 * 格式：---REVIEW: type | Title---...---END REVIEW---
 */
@Component
public class ReviewBlockParser {

    private static final Pattern BLOCK = Pattern.compile(
            "---REVIEW:\\s*(\\S+)\\s*\\|\\s*(.+?)---\\s*\\n(.*?)---END REVIEW---",
            Pattern.DOTALL
    );

    private static final Pattern OPTIONS_LINE = Pattern.compile("(?m)^OPTIONS:\\s*(.+)$");
    private static final Pattern PAGES_LINE   = Pattern.compile("(?m)^PAGES:\\s*(.+)$");
    private static final Pattern SEARCH_LINE  = Pattern.compile("(?m)^SEARCH:\\s*(.+)$");

    /**
     * 从 LLM 输出中提取所有 REVIEW block。
     */
    public List<ReviewItem> parse(String llmOutput) {
        List<ReviewItem> result = new ArrayList<>();
        if (llmOutput == null || llmOutput.isBlank()) return result;

        Matcher m = BLOCK.matcher(llmOutput);
        while (m.find()) {
            String type        = m.group(1).trim();
            String title       = m.group(2).trim();
            String body        = m.group(3);

            List<String> options  = extractPipeSplit(body, OPTIONS_LINE);
            List<String> pages    = extractCommaSplit(body, PAGES_LINE);
            List<String> searches = extractPipeSplit(body, SEARCH_LINE);
            String description    = extractDescription(body);

            result.add(new ReviewItem(type, title, description, options, pages, searches));
        }
        return result;
    }

    // ---- private helpers ----

    private List<String> extractPipeSplit(String body, Pattern linePattern) {
        Matcher m = linePattern.matcher(body);
        if (!m.find()) return List.of();
        return splitTrimmed(m.group(1), "\\|");
    }

    private List<String> extractCommaSplit(String body, Pattern linePattern) {
        Matcher m = linePattern.matcher(body);
        if (!m.find()) return List.of();
        // 支持逗号或换行分隔
        String raw = m.group(1).trim();
        return splitTrimmed(raw, "[,\\n]");
    }

    private List<String> splitTrimmed(String raw, String delimiter) {
        return Arrays.stream(raw.split(delimiter))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private String extractDescription(String body) {
        // description = body 中去掉 OPTIONS/PAGES/SEARCH 行后剩余部分
        String desc = body
                .replaceAll("(?m)^OPTIONS:.*$", "")
                .replaceAll("(?m)^PAGES:.*$", "")
                .replaceAll("(?m)^SEARCH:.*$", "")
                .trim();
        return desc;
    }
}
