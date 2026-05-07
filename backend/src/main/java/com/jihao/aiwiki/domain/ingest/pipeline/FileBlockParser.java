package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LLM 输出 FILE block 解析器。
 * 使用行级状态机，正确处理 CRLF、fence 嵌套、未关闭 block、空路径等边界情况。
 */
@Component
public class FileBlockParser {

    private static final Pattern OPENER = Pattern.compile("^---FILE:\\s*(.+?)\\s*---\\s*$");
    private static final Pattern CLOSER = Pattern.compile("^---END FILE---\\s*$");
    private static final Pattern FENCE  = Pattern.compile("^(```|~~~).*$");

    /**
     * 从 LLM 输出中提取所有 FILE block，同时收集解析警告。
     */
    public ParseResult parseWithWarnings(String llmOutput) {
        List<FileBlock> blocks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (llmOutput == null || llmOutput.isBlank()) {
            return new ParseResult(blocks, warnings);
        }

        String normalized = llmOutput.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        boolean inBlock = false;
        String currentPath = null;
        List<String> contentLines = new ArrayList<>();
        int fenceDepth = 0;

        for (String line : lines) {
            if (!inBlock) {
                var openerMatcher = OPENER.matcher(line);
                if (openerMatcher.matches()) {
                    String path = openerMatcher.group(1).trim();
                    if (path.isEmpty()) {
                        warnings.add("空路径 FILE block，已跳过");
                        continue;
                    }
                    inBlock = true;
                    currentPath = path;
                    contentLines = new ArrayList<>();
                    fenceDepth = 0;
                }
            } else {
                if (FENCE.matcher(line).matches()) {
                    fenceDepth ^= 1;
                }
                if (fenceDepth == 0 && CLOSER.matcher(line).matches()) {
                    blocks.add(new FileBlock(currentPath, String.join("\n", contentLines)));
                    inBlock = false;
                    currentPath = null;
                    contentLines = new ArrayList<>();
                } else {
                    contentLines.add(line);
                }
            }
        }

        if (inBlock) {
            warnings.add("未关闭的 FILE block: " + currentPath);
        }

        return new ParseResult(blocks, warnings);
    }

    /**
     * 兼容旧调用方，丢弃 warnings 只返回 blocks。
     */
    public List<FileBlock> parse(String llmOutput) {
        return parseWithWarnings(llmOutput).blocks();
    }
}
