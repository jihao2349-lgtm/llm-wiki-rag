package com.jihao.aiwiki.domain.ingest;

import com.jihao.aiwiki.domain.ingest.pipeline.FileBlock;
import com.jihao.aiwiki.domain.ingest.pipeline.FileBlockParser;
import com.jihao.aiwiki.domain.ingest.pipeline.ParseResult;
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
    void parse_missingEndMarker_returnsEmptyBlocksWithWarning() {
        String llmOutput = """
                ---FILE: wiki/concepts/agent.md---
                content without end marker
                """;

        ParseResult result = parser.parseWithWarnings(llmOutput);
        assertThat(result.blocks()).isEmpty();
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("未关闭");
    }

    @Test
    void parse_nullInput_returnsEmpty() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
    }

    @Test
    void parse_crlfInput_parsedCorrectly() {
        String llmOutput = "---FILE: wiki/concepts/agent.md---\r\n# Agent\r\n---END FILE---\r\n";

        List<FileBlock> blocks = parser.parse(llmOutput);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getPath()).isEqualTo("wiki/concepts/agent.md");
        assertThat(blocks.get(0).getContent()).doesNotContain("\r");
    }

    @Test
    void parse_fenceInsideBlock_doesNotTriggerEarlyClose() {
        String llmOutput = """
                ---FILE: wiki/concepts/transformer.md---
                ---
                type: concept
                ---
                # Transformer

                ```python
                ---END FILE---
                this line is inside the fence, not a closer
                ```
                ---END FILE---
                """;

        List<FileBlock> blocks = parser.parse(llmOutput);

        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getContent()).contains("this line is inside the fence");
    }

    @Test
    void parse_emptyPath_skippedWithWarning() {
        String llmOutput = "---FILE:   ---\ncontent\n---END FILE---\n";

        ParseResult result = parser.parseWithWarnings(llmOutput);

        assertThat(result.blocks()).isEmpty();
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("空路径");
    }

    @Test
    void parse_unclosedBlock_warningRecorded() {
        String llmOutput = """
                ---FILE: wiki/concepts/agent.md---
                some content
                """;

        ParseResult result = parser.parseWithWarnings(llmOutput);

        assertThat(result.blocks()).isEmpty();
        assertThat(result.warnings()).anyMatch(w -> w.contains("wiki/concepts/agent.md"));
    }
}
