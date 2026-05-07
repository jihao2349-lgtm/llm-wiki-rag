package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.ReviewBlockParser;
import com.jihao.aiwiki.domain.ingest.pipeline.ReviewItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewBlockParserTest {

    private final ReviewBlockParser parser = new ReviewBlockParser();

    @Test
    void parse_singleContradictionBlock() {
        String llmOutput = """
                ---REVIEW: contradiction | Conflicting Definition---
                The transformer definition in concepts/transformer.md contradicts the one in sources/paper.md.
                OPTIONS: Update Page | Skip
                PAGES: wiki/concepts/transformer.md
                SEARCH: transformer definition | attention mechanism
                ---END REVIEW---
                """;

        List<ReviewItem> items = parser.parse(llmOutput);

        assertThat(items).hasSize(1);
        ReviewItem item = items.get(0);
        assertThat(item.type()).isEqualTo("contradiction");
        assertThat(item.title()).isEqualTo("Conflicting Definition");
        assertThat(item.options()).containsExactly("Update Page", "Skip");
        assertThat(item.pages()).containsExactly("wiki/concepts/transformer.md");
        assertThat(item.searches()).containsExactly("transformer definition", "attention mechanism");
        assertThat(item.description()).contains("contradicts");
    }

    @Test
    void parse_multipleBlocks() {
        String llmOutput = """
                ---REVIEW: duplicate | Possible Duplicate---
                Entity vaswani might already exist.
                OPTIONS: Merge | Create Page | Skip
                PAGES: wiki/entities/vaswani.md
                SEARCH: vaswani
                ---END REVIEW---

                ---REVIEW: suggestion | Missing Overview---
                Consider updating overview.md with new content.
                OPTIONS: Update | Skip
                PAGES: wiki/overview.md
                SEARCH: overview
                ---END REVIEW---
                """;

        List<ReviewItem> items = parser.parse(llmOutput);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).type()).isEqualTo("duplicate");
        assertThat(items.get(1).type()).isEqualTo("suggestion");
    }

    @Test
    void parse_noReviewBlocks_returnsEmpty() {
        String llmOutput = """
                ---FILE: wiki/concepts/agent.md---
                ---
                type: concept
                title: Agent
                ---
                # Agent
                ---END FILE---
                """;

        assertThat(parser.parse(llmOutput)).isEmpty();
    }

    @Test
    void parse_nullOrBlank_returnsEmpty() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void parse_missingPageBlock_typeAndTitleParsed() {
        String llmOutput = """
                ---REVIEW: missing-page | No Source Page---
                wiki/sources/paper.md was not generated.
                OPTIONS: Create Page | Skip
                SEARCH: paper source
                ---END REVIEW---
                """;

        List<ReviewItem> items = parser.parse(llmOutput);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).type()).isEqualTo("missing-page");
        assertThat(items.get(0).title()).isEqualTo("No Source Page");
        assertThat(items.get(0).pages()).isEmpty();
        assertThat(items.get(0).searches()).containsExactly("paper source");
    }
}
