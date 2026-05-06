package com.jihao.aiwiki.domain.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 纯文本、Markdown、JSON、CSV 解析器，按 UTF-8 读取全文。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class TxtMarkdownParser implements DocumentParser {

    /** 支持的文件扩展名集合 */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".txt", ".md", ".json", ".csv");

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
}
