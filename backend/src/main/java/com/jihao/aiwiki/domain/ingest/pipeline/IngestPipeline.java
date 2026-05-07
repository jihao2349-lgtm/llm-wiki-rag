package com.jihao.aiwiki.domain.ingest.pipeline;

import com.jihao.aiwiki.domain.ingest.queue.IngestTaskRunContext;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStage;
import com.jihao.aiwiki.domain.llm.LlmChatRequest;
import com.jihao.aiwiki.domain.llm.LlmChatResponse;
import com.jihao.aiwiki.domain.llm.LlmClient;
import com.jihao.aiwiki.domain.llm.LlmMessage;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.entity.SourceDocumentDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 两阶段摄入流水线。
 * 阶段一：资料分析（JSON）。阶段二：生成 FILE block 并写入 wiki/。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class IngestPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestPipeline.class);

    private final LlmClient llmClient;
    private final VaultFileService fileService;
    private final FileBlockParser fileBlockParser;
    private final MarkdownFrontmatterValidator frontmatterValidator;
    private final IngestContentSanitizer contentSanitizer;
    private final SourcesMerger sourcesMerger;
    private final ReviewBlockParser reviewBlockParser;

    public IngestPipeline(LlmClient llmClient,
                          VaultFileService fileService,
                          FileBlockParser fileBlockParser,
                          MarkdownFrontmatterValidator frontmatterValidator,
                          IngestContentSanitizer contentSanitizer,
                          SourcesMerger sourcesMerger,
                          ReviewBlockParser reviewBlockParser) {
        this.llmClient = llmClient;
        this.fileService = fileService;
        this.fileBlockParser = fileBlockParser;
        this.frontmatterValidator = frontmatterValidator;
        this.contentSanitizer = contentSanitizer;
        this.sourcesMerger = sourcesMerger;
        this.reviewBlockParser = reviewBlockParser;
    }

    /**
     * 执行两阶段摄入。
     *
     * @param context     任务上下文
     * @param vault       Vault 项目
     * @param source      资料实体
     * @param llmRequest  基础 LLM 请求模板（含 apiKey、model、baseUrl）
     */
    public void run(IngestTaskRunContext context,
                    VaultProjectDO vault,
                    SourceDocumentDO source,
                    LlmChatRequest llmRequest,
                    String language) throws Exception {

        IngestTaskDO task = context.getTask();
        Path vaultRoot = Path.of(vault.getPath());

        // ---- Phase 1: Analyze ----
        context.updateProgress(IngestTaskStage.ANALYZING, 10);
        if (context.isCancellationRequested()) return;

        String sourceText = loadSourceText(vaultRoot, source);
        String purposeMd = readSafe(vaultRoot, "purpose.md");
        String schemaMd = readSafe(vaultRoot, "schema.md");
        String indexMd = readSafe(vaultRoot, "wiki/index.md");

        String analyzePrompt = buildAnalyzePrompt(purposeMd, schemaMd, indexMd, sourceText, source);
        String analysisJson = callLlm(llmRequest, analyzePrompt, false);
        context.updateProgress(IngestTaskStage.ANALYZING, 50);

        // ---- Phase 2: Generate FILE blocks ----
        if (context.isCancellationRequested()) return;
        context.updateProgress(IngestTaskStage.WRITING, 55);

        String sourceSlug = slugFromPath(source.getOriginalPath());
        String today = LocalDate.now().toString();
        String generatePrompt = buildGeneratePrompt(analysisJson, sourceText, sourceSlug, today, source.getTitle(), language);
        String llmOutput = callLlm(llmRequest, generatePrompt, false);

        List<FileBlock> blocks = fileBlockParser.parse(llmOutput);
        if (blocks.isEmpty()) {
            throw new IllegalStateException("LLM returned no FILE blocks");
        }

        // ---- Per-block 清洗、校验（warn-and-skip）、三分支写入 ----
        List<String> warnings = new ArrayList<>();
        List<String> writtenFiles = new ArrayList<>();

        for (FileBlock block : blocks) {
            if (context.isCancellationRequested()) return;

            String cleanedContent = contentSanitizer.sanitize(block.getContent());

            String pathErr = frontmatterValidator.validatePath(block.getPath());
            String fmErr = frontmatterValidator.validateFrontmatter(cleanedContent);
            if (pathErr != null || fmErr != null) {
                warnings.add("[SKIP] " + block.getPath() + ": " + (pathErr != null ? pathErr : fmErr));
                continue;
            }

            if ("wiki/log.md".equals(block.getPath())) {
                String existing = readSafe(vaultRoot, "wiki/log.md");
                fileService.writeStringAtomically(vaultRoot, "wiki/log.md", existing + "\n" + cleanedContent);
            } else if ("wiki/index.md".equals(block.getPath()) || "wiki/overview.md".equals(block.getPath())) {
                fileService.writeStringAtomically(vaultRoot, block.getPath(), cleanedContent);
            } else {
                String existing = readSafe(vaultRoot, block.getPath());
                String merged = sourcesMerger.mergeSourcesIntoContent(cleanedContent, existing.isBlank() ? null : existing);
                fileService.writeStringAtomically(vaultRoot, block.getPath(), merged);
            }

            writtenFiles.add(block.getPath());
            context.updateWrittenFiles(writtenFiles.stream()
                    .collect(Collectors.joining(",", "[\"", "\"]"))
                    .replace(",", "\",\""));
        }

        if (!warnings.isEmpty()) {
            log.warn("Ingest block warnings: {}", warnings);
        }

        List<ReviewItem> reviews = reviewBlockParser.parse(llmOutput);
        if (!reviews.isEmpty()) {
            log.info("Ingest REVIEW items ({}): {}", reviews.size(), reviews);
        }

        // ---- source summary 兜底：LLM 未生成摘要页时自动创建 stub ----
        boolean hasSourcePage = writtenFiles.stream().anyMatch(p -> p.startsWith("wiki/sources/"));
        if (!hasSourcePage) {
            String stubPath = "wiki/sources/" + sourceSlug + ".md";
            String analysisPreview = analysisJson.length() > 3000
                    ? analysisJson.substring(0, 3000) : analysisJson;
            String sourceRef = source.getOriginalPath() != null ? source.getOriginalPath() : sourceSlug;
            String stubContent = """
                    ---
                    type: source
                    title: "Source: %s"
                    created: %s
                    updated: %s
                    sources: ["%s"]
                    tags: []
                    related: []
                    ---

                    # Source: %s

                    %s
                    """.formatted(source.getTitle(), today, today, sourceRef,
                    source.getTitle(), analysisPreview);
            fileService.writeStringAtomically(vaultRoot, stubPath, stubContent);
            writtenFiles.add(stubPath);
            log.info("Generated fallback source summary: {}", stubPath);
        }

        context.updateProgress(IngestTaskStage.INDEXING, 90);
    }

    // ---- Private helpers ----

    private String loadSourceText(Path vaultRoot, SourceDocumentDO source) {
        if (source.getExtractedTextPath() != null) {
            try {
                byte[] bytes = fileService.readBytes(vaultRoot, source.getExtractedTextPath());
                String text = new String(bytes, StandardCharsets.UTF_8);
                return text.length() > 30000 ? text.substring(0, 30000) : text;
            } catch (Exception e) {
                log.warn("Could not read extracted text for source {}", source.getId());
            }
        }
        return source.getTitle() != null ? source.getTitle() : "";
    }

    private String readSafe(Path vaultRoot, String path) {
        try {
            return fileService.readString(vaultRoot, path);
        } catch (Exception e) {
            return "";
        }
    }

    private String callLlm(LlmChatRequest template, String userPrompt, boolean stream) {
        LlmChatRequest req = LlmChatRequest.builder()
                .provider(template.getProvider())
                .baseUrl(template.getBaseUrl())
                .apiKey(template.getApiKey())
                .model(template.getModel())
                .temperature(template.getTemperature() != null ? template.getTemperature() : BigDecimal.valueOf(0.3))
                .messages(List.of(
                        new LlmMessage("system", buildSystemPrompt()),
                        new LlmMessage("user", userPrompt)
                ))
                .stream(stream)
                .build();
        LlmChatResponse response = llmClient.chat(req);
        return response.getContent();
    }

    private String buildSystemPrompt() {
        return """
                You are an AI knowledge base assistant. Your job is to analyze source documents and generate structured Obsidian-compatible Markdown wiki pages.
                Always follow the exact FILE block format requested. Never output absolute paths, '../', or paths outside wiki/. Always include frontmatter with type, title, sources, and updated fields.
                """;
    }

    private String buildAnalyzePrompt(String purpose, String schema, String index,
                                       String sourceText, SourceDocumentDO source) {
        return """
                You are analyzing a source document to prepare knowledge extraction.

                [Purpose of this wiki]
                %s

                [Wiki Schema]
                %s

                [Current Wiki Index]
                %s

                [Folder Context]
                Path: %s

                [Source Document]
                Title: %s

                %s

                ---

                Analyze the document and respond in the following structured format (Markdown, not JSON):

                ## Key Entities
                For each: Name | Type (person/org/tool) | Role in this document | Already in wiki index? (yes/no)

                ## Key Concepts
                For each: Name | Short definition | Why important here | Already in wiki index? (yes/no)

                ## Main Arguments & Findings
                For each: Core claim | Supporting evidence | Evidence strength (strong/moderate/weak)

                ## Connections to Existing Wiki
                How this document relates to existing content (reinforces/challenges/extends which pages)

                ## Contradictions & Tensions
                Conflicts with existing wiki; internal tensions in the document

                ## Recommendations
                Which pages to create or update | Key content focus | Open questions worth flagging
                """.formatted(purpose, schema, index, source.getOriginalPath(), source.getTitle(), sourceText);
    }

    private String buildGeneratePrompt(String analysisJson, String sourceText,
                                        String sourceSlug, String today, String title, String language) {
        return """
                Language instruction: %s. All generated wiki page content MUST be written in this language.

                FRONTMATTER RULES (mandatory):
                - First line must be exactly `---`, never use ```yaml fences
                - Each field on its own line; arrays use inline format: tags: [a, b]
                - `related` uses slugs only, no wiki/ prefix, no .md suffix
                - `sources` must include the current source filename
                - Wikilinks [[...]] only in body, never in frontmatter values

                REQUIRED FILES (generate all of them):
                1. wiki/sources/%s.md (MANDATORY — exact path)
                2. wiki/entities/<slug>.md for each key entity
                3. wiki/concepts/<slug>.md for each key concept
                4. wiki/index.md — append new entries, preserve ALL existing entries
                5. wiki/log.md — only the new entry: ## [%s] ingest | %s
                6. wiki/overview.md — 2-5 paragraph overview of the entire wiki content

                FILE block format (strict):
                ---FILE: wiki/path/file.md---
                <content>
                ---END FILE---

                For items requiring human judgment, use REVIEW blocks (placed outside FILE blocks):
                ---REVIEW: contradiction | Title---
                Description of the issue
                OPTIONS: Create Page | Skip
                PAGES: wiki/relevant-page.md
                SEARCH: query1 | query2
                ---END REVIEW---
                Types: contradiction / duplicate / missing-page / suggestion

                OUTPUT RULES:
                - First character of your response must be `-` (start of ---FILE:)
                - No preamble, summary, or analysis text outside FILE/REVIEW blocks
                - Nothing between FILE blocks except REVIEW blocks

                Analysis:
                %s

                Source text:
                %s

                Language instruction (repeated): %s. Generate ALL content in this language.
                """.formatted(language, sourceSlug, today, title, analysisJson, sourceText, language);
    }

    private String slugFromPath(String path) {
        if (path == null) return "source";
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
