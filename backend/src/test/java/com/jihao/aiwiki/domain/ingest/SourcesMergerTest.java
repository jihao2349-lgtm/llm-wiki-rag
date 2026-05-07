package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.SourcesMerger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourcesMergerTest {

    private final SourcesMerger merger = new SourcesMerger();

    // ---- parseSources ----

    @Test
    void parseSources_inlineFormat() {
        String content = """
                ---
                type: concept
                sources: ["paper.pdf", "slides.pptx"]
                ---
                # Body""";

        List<String> sources = merger.parseSources(content);

        assertThat(sources).containsExactly("paper.pdf", "slides.pptx");
    }

    @Test
    void parseSources_multilineFormat() {
        String content = """
                ---
                type: concept
                sources:
                  - paper.pdf
                  - slides.pptx
                ---
                # Body""";

        List<String> sources = merger.parseSources(content);

        assertThat(sources).containsExactly("paper.pdf", "slides.pptx");
    }

    @Test
    void parseSources_emptyInline() {
        String content = "---\nsources: []\n---\n# Body";
        assertThat(merger.parseSources(content)).isEmpty();
    }

    @Test
    void parseSources_noSourcesField() {
        String content = "---\ntype: concept\n---\n# Body";
        assertThat(merger.parseSources(content)).isEmpty();
    }

    // ---- mergeSourcesLists ----

    @Test
    void mergeSourcesLists_noOverlap() {
        List<String> result = merger.mergeSourcesLists(
                List.of("a.pdf"), List.of("b.pdf"));

        assertThat(result).containsExactly("a.pdf", "b.pdf");
    }

    @Test
    void mergeSourcesLists_withOverlap_deduped() {
        List<String> result = merger.mergeSourcesLists(
                List.of("a.pdf", "b.pdf"), List.of("b.pdf", "c.pdf"));

        assertThat(result).containsExactly("a.pdf", "b.pdf", "c.pdf");
    }

    @Test
    void mergeSourcesLists_caseInsensitiveDedupe() {
        List<String> result = merger.mergeSourcesLists(
                List.of("Paper.pdf"), List.of("paper.pdf"));

        // existing 顺序优先，incoming 重复忽略
        assertThat(result).containsExactly("Paper.pdf");
    }

    @Test
    void mergeSourcesLists_existingOrderPreserved() {
        List<String> result = merger.mergeSourcesLists(
                List.of("z.pdf", "a.pdf"), List.of("b.pdf"));

        assertThat(result).containsExactly("z.pdf", "a.pdf", "b.pdf");
    }

    // ---- mergeSourcesIntoContent ----

    @Test
    void mergeSourcesIntoContent_existingNull_returnsNewContent() {
        String newContent = "---\nsources: [\"new.pdf\"]\n---\n# Body";

        String result = merger.mergeSourcesIntoContent(newContent, null);

        assertThat(result).isEqualTo(newContent);
    }

    @Test
    void mergeSourcesIntoContent_existingEmpty_returnsNewContent() {
        String newContent = "---\nsources: [\"new.pdf\"]\n---\n# Body";

        String result = merger.mergeSourcesIntoContent(newContent, "");

        assertThat(result).isEqualTo(newContent);
    }

    @Test
    void mergeSourcesIntoContent_mergesAndUnifiesInlineFormat() {
        String existing = """
                ---
                type: concept
                sources:
                  - old.pdf
                ---
                # Old Body""";

        String newContent = """
                ---
                type: concept
                sources: ["new.pdf"]
                ---
                # New Body""";

        String result = merger.mergeSourcesIntoContent(newContent, existing);

        assertThat(result).contains("sources: [\"old.pdf\", \"new.pdf\"]");
        assertThat(result).contains("# New Body");
    }

    @Test
    void mergeSourcesIntoContent_inlineInlineOverlapDeduped() {
        String existing = "---\nsources: [\"a.pdf\", \"b.pdf\"]\n---\n# Old";
        String newContent = "---\nsources: [\"b.pdf\", \"c.pdf\"]\n---\n# New";

        String result = merger.mergeSourcesIntoContent(newContent, existing);

        assertThat(result).contains("sources: [\"a.pdf\", \"b.pdf\", \"c.pdf\"]");
    }
}
