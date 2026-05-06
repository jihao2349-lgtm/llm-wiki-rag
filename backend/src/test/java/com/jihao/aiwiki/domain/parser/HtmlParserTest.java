package com.jihao.aiwiki.domain.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlParserTest {

    private final HtmlParser parser = new HtmlParser();

    @Test
    void supports_htmlExtensions() {
        assertThat(parser.supports("page.html")).isTrue();
        assertThat(parser.supports("page.htm")).isTrue();
        assertThat(parser.supports("page.txt")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void extractText_returnsTitle_and_body() throws IOException {
        String html = "<html><head><title>Hello World</title></head>"
                + "<body><p>This is content.</p></body></html>";
        var input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));

        String result = parser.extractText(input, "page.html");

        assertThat(result).contains("Hello World");
        assertThat(result).contains("This is content.");
    }

    @Test
    void extractText_noTitle_returnsBody() throws IOException {
        String html = "<html><body><p>body only</p></body></html>";
        var input = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));

        String result = parser.extractText(input, "page.html");

        assertThat(result).contains("body only");
    }
}
