package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.MarkdownFrontmatterValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownFrontmatterValidatorTest {

    private final MarkdownFrontmatterValidator validator = new MarkdownFrontmatterValidator();

    // ---- Path tests ----

    @Test
    void validatePath_valid() {
        assertThat(validator.validatePath("wiki/concepts/agent.md")).isNull();
        assertThat(validator.validatePath("wiki/sources/doc.md")).isNull();
    }

    @Test
    void validatePath_notWikiPrefix_rejected() {
        assertThat(validator.validatePath("raw/sources/evil.md")).isNotNull();
        assertThat(validator.validatePath("../../etc/passwd")).isNotNull();
    }

    @Test
    void validatePath_dotdot_rejected() {
        assertThat(validator.validatePath("wiki/../secret.md")).isNotNull();
    }

    @Test
    void validatePath_backslash_rejected() {
        assertThat(validator.validatePath("wiki\\evil.md")).isNotNull();
    }

    @Test
    void validatePath_nullOrEmpty_rejected() {
        assertThat(validator.validatePath(null)).isNotNull();
        assertThat(validator.validatePath("")).isNotNull();
        assertThat(validator.validatePath("   ")).isNotNull();
    }

    // ---- Frontmatter tests ----

    @Test
    void validateFrontmatter_allRequired_valid() {
        String md = """
                ---
                title: Agent Memory
                type: concept
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                # Agent Memory
                """;
        assertThat(validator.validateFrontmatter(md)).isNull();
    }

    @Test
    void validateFrontmatter_missingTitle_rejected() {
        String md = """
                ---
                type: concept
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                """;
        assertThat(validator.validateFrontmatter(md)).contains("title");
    }

    @Test
    void validateFrontmatter_missingType_rejected() {
        String md = """
                ---
                title: X
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                """;
        assertThat(validator.validateFrontmatter(md)).contains("type");
    }

    @Test
    void validateFrontmatter_invalidType_rejected() {
        String md = """
                ---
                title: X
                type: unknown_type
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                """;
        assertThat(validator.validateFrontmatter(md)).contains("type");
    }

    @Test
    void validateFrontmatter_noFrontmatter_rejected() {
        String md = "# Just a title\nNo frontmatter here.";
        assertThat(validator.validateFrontmatter(md)).contains("missing frontmatter");
    }
}
