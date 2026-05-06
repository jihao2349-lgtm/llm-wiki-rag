package com.jihao.aiwiki.domain.parser;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PPTX 文件解析器，使用 Apache POI 提取幻灯片文本。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class PptxParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".pptx");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        try (var show = new XMLSlideShow(input)) {
            List<String> lines = new ArrayList<>();
            for (var slide : show.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            lines.add(text.trim());
                        }
                    }
                }
            }
            return String.join("\n", lines);
        }
    }
}
