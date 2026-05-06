package com.jihao.aiwiki.domain.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownFrontmatterParserTest {

    private final MarkdownFrontmatterParser parser = new MarkdownFrontmatterParser();

    @Test
    void parse_fullFrontmatter() {
        String md = """
                ---
                title: Agent Memory
                type: concept
                tags:
                  - ai
                  - memory
                related:
                  - wiki/concepts/llm.md
                updated: 2026-05-01
                ---
                # Agent Memory
                Body text here.
                """;

        ParsedFrontmatter fm = parser.parse(md);

        assertThat(fm.getTitle()).isEqualTo("Agent Memory");
        assertThat(fm.getType()).isEqualTo("concept");
        assertThat(fm.getTags()).containsExactly("ai", "memory");
        assertThat(fm.getRelated()).containsExactly("wiki/concepts/llm.md");
        assertThat(fm.getBody()).contains("# Agent Memory");
    }

    @Test
    void parse_noFrontmatter_returnsNullFields() {
        String md = "# Just a title\n\nsome content";
        ParsedFrontmatter fm = parser.parse(md);

        assertThat(fm.getTitle()).isNull();
        assertThat(fm.getType()).isNull();
        assertThat(fm.getBody()).contains("# Just a title");
    }

    @Test
    void parse_emptyString_returnsEmpty() {
        ParsedFrontmatter fm = parser.parse("");
        assertThat(fm.getTitle()).isNull();
        assertThat(fm.getBody()).isEmpty();
    }

    @Test
    void parse_nullInput_returnsEmpty() {
        ParsedFrontmatter fm = parser.parse(null);
        assertThat(fm.getTitle()).isNull();
    }
}
