package com.jihao.aiwiki.domain.parser;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTML 文件解析器，使用 Jsoup 提取标题与正文文本。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class HtmlParser implements DocumentParser {

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".html") || lower.endsWith(".htm");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        var doc = Jsoup.parse(input, "UTF-8", "");
        return doc.title() + "\n\n" + doc.body().text();
    }
}
