package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.FileBlock;
import com.jihao.aiwiki.domain.ingest.pipeline.FileBlockParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileBlockParserTest {

    private final FileBlockParser parser = new FileBlockParser();

    @Test
    void parse_singleBlock() {
        String llmOutput = """
                ---FILE: wiki/concepts/agent.md---
                ---
                title: Agent
                type: concept
                sources:
                  - raw/sources/agent.pdf
                updated: 2026-05-06
                ---

                # Agent
                Body text here.
                ---END FILE---
                """;

        List<FileBlock> blocks = parser.parse(llmOutput);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getPath()).isEqualTo("wiki/concepts/agent.md");
        assertThat(blocks.get(0).getContent()).contains("# Agent");
    }

    @Test
    void parse_multipleBlocks() {
        String llmOutput = """
                ---FILE: wiki/concepts/memory.md---
                ---
                title: Memory
                type: concept
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                Memory content.
                ---END FILE---

                ---FILE: wiki/entities/openai.md---
                ---
                title: OpenAI
                type: entity
                sources:
                  - raw/sources/paper.pdf
                updated: 2026-05-06
                ---
                OpenAI content.
                ---END FILE---
                """;

        List<FileBlock> blocks = parser.parse(llmOutput);

        assertThat(blocks).hasSize(2);
        assertThat(blocks.get(0).getPath()).isEqualTo("wiki/concepts/memory.md");
        assertThat(blocks.get(1).getPath()).isEqualTo("wiki/entities/openai.md");
    }

    @Test
    void parse_missingEndMarker_returnsEmpty() {
        String llmOutput = """
                ---FILE: wiki/concepts/agent.md---
                content without end marker
                """;

        List<FileBlock> blocks = parser.parse(llmOutput);
        assertThat(blocks).isEmpty();
    }

    @Test
    void parse_nullInput_returnsEmpty() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
    }
}
