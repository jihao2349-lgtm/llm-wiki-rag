package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.IngestContentSanitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngestContentSanitizerTest {

    private final IngestContentSanitizer sanitizer = new IngestContentSanitizer();

    @Test
    void stripOuterFence_yamlFence() {
        String input = """
                ```yaml
                ---
                type: entity
                ---
                # Body
                ```""";

        String result = sanitizer.sanitize(input);

        assertThat(result).startsWith("---");
        assertThat(result).contains("# Body");
        assertThat(result).doesNotContain("```");
    }

    @Test
    void stripOuterFence_plainFence() {
        String input = "```\n---\ntype: concept\n---\n# Title\n```";

        String result = sanitizer.sanitize(input);

        assertThat(result).startsWith("---");
        assertThat(result).doesNotContain("```");
    }

    @Test
    void stripFrontmatterPrefix() {
        String input = """
                frontmatter:
                ---
                type: entity
                ---
                # Body""";

        String result = sanitizer.sanitize(input);

        assertThat(result).startsWith("---");
        assertThat(result).doesNotContain("frontmatter:");
    }

    @Test
    void stripFrontmatterPrefix_caseInsensitive() {
        String input = "Frontmatter:\n---\ntype: source\n---\n# Body";

        String result = sanitizer.sanitize(input);

        assertThat(result).startsWith("---");
    }

    @Test
    void fixWikilinkList_inlineSingletons() {
        String input = """
                ---
                type: concept
                related: [[transformer]], [[attention]]
                ---
                # Body""";

        String result = sanitizer.sanitize(input);

        assertThat(result).contains("related: [\"[[transformer]]\", \"[[attention]]\"]");
    }

    @Test
    void fixWikilinkList_singleWikilink() {
        String input = """
                ---
                type: concept
                related: [[transformer]]
                ---
                # Body""";

        String result = sanitizer.sanitize(input);

        assertThat(result).contains("related: [\"[[transformer]]\"]");
    }

    @Test
    void noChange_whenContentIsAlreadyClean() {
        String input = """
                ---
                type: concept
                title: Clean
                related: []
                ---
                # Body""";

        String result = sanitizer.sanitize(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void nullInput_returnsNull() {
        assertThat(sanitizer.sanitize(null)).isNull();
    }
}
