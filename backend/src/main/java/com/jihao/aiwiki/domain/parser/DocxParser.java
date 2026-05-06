package com.jihao.aiwiki.domain.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

/**
 * DOCX 文件解析器，使用 Apache POI 提取段落文本。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class DocxParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".docx");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        try (var doc = new XWPFDocument(input)) {
            return doc.getParagraphs().stream()
                    .map(p -> p.getText())
                    .collect(Collectors.joining("\n"));
        }
    }
}
