package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 输出 FILE block 解析器。
 * 支持单个或多个 {@code ---FILE: path---...---END FILE---} 块。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class FileBlockParser {

    private static final Pattern BLOCK_PATTERN = Pattern.compile(
            "---FILE:\\s*([^\\r\\n]+?)\\s*---\\r?\\n(.*?)---END FILE---",
            Pattern.DOTALL
    );

    /**
     * 从 LLM 输出中提取所有 FILE block。
     *
     * @param llmOutput LLM 原始输出
     * @return 解析出的 FileBlock 列表，无法解析时返回空列表
     */
    public List<FileBlock> parse(String llmOutput) {
        List<FileBlock> blocks = new ArrayList<>();
        if (llmOutput == null || llmOutput.isBlank()) {
            return blocks;
        }
        Matcher matcher = BLOCK_PATTERN.matcher(llmOutput);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String content = matcher.group(2);
            blocks.add(new FileBlock(path, content));
        }
        return blocks;
    }
}
