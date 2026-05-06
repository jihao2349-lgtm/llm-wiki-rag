package com.jihao.aiwiki.domain.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 文档解析器，使用 Apache PDFBox 提取文本。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        byte[] bytes = input.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
