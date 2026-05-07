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

    public IngestPipeline(LlmClient llmClient,
                          VaultFileService fileService,
                          FileBlockParser fileBlockParser,
                          MarkdownFrontmatterValidator frontmatterValidator) {
        this.llmClient = llmClient;
        this.fileService = fileService;
        this.fileBlockParser = fileBlockParser;
        this.frontmatterValidator = frontmatterValidator;
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
                    LlmChatRequest llmRequest) throws Exception {

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
        String generatePrompt = buildGeneratePrompt(analysisJson, sourceText, sourceSlug, today, source.getTitle());
        String llmOutput = callLlm(llmRequest, generatePrompt, false);

        List<FileBlock> blocks = fileBlockParser.parse(llmOutput);
        if (blocks.isEmpty()) {
            throw new IllegalStateException("LLM returned no FILE blocks");
        }

        // ---- Validate all blocks before writing ----
        List<String> errors = new ArrayList<>();
        for (FileBlock block : blocks) {
            String pathErr = frontmatterValidator.validatePath(block.getPath());
            if (pathErr != null) errors.add("path error [" + block.getPath() + "]: " + pathErr);
            String fmErr = frontmatterValidator.validateFrontmatter(block.getContent());
            if (fmErr != null) errors.add("frontmatter error [" + block.getPath() + "]: " + fmErr);
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("FILE block validation failed: " + String.join("; ", errors));
        }

        // ---- Write via tmp/ then atomic replace ----
        String tmpBase = ".ai-wiki/tmp/" + task.getTaskId() + "/";
        List<String> writtenFiles = new ArrayList<>();

        for (FileBlock block : blocks) {
            if (context.isCancellationRequested()) return;
            String tmpPath = tmpBase + block.getPath();
            fileService.writeStringAtomically(vaultRoot, tmpPath, block.getContent());
            fileService.writeStringAtomically(vaultRoot, block.getPath(), block.getContent());
            writtenFiles.add(block.getPath());
            context.updateWrittenFiles(writtenFiles.stream()
                    .collect(Collectors.joining(",", "[\"", "\"]"))
                    .replace(",", "\",\""));
        }

        // ---- Update wiki/log.md ----
        appendWikiLog(vaultRoot, task.getTaskId(), source.getTitle(), writtenFiles, today);

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
                Analyze the following source document and output a JSON analysis.

                ## Purpose
                %s

                ## Schema
                %s

                ## Current Wiki Index
                %s

                ## Source Document
                Title: %s
                Path: %s

                Content:
                %s

                Output JSON with keys: summary, entities, concepts, keyPoints, relatedPages, conflicts, reviewQuestions, proposedPages.
                """.formatted(purpose, schema, index, source.getTitle(), source.getOriginalPath(), sourceText);
    }

    private String buildGeneratePrompt(String analysisJson, String sourceText,
                                        String sourceSlug, String today, String title) {
        return """
                Based on the analysis below, generate Obsidian Markdown wiki pages in FILE block format.

                Analysis:
                %s

                Source text (truncated):
                %s

                Rules:
                - Each file must use the format: ---FILE: wiki/path/file.md---\\n<content>\\n---END FILE---
                - All paths must start with wiki/
                - Each page must have frontmatter with type, title, sources, updated
                - sources must reference raw/sources/ paths
                - The source slug is: %s
                - Today's date: %s
                - Original title: %s
                - type must be one of: source, concept, entity, synthesis, question, index, overview, log
                - Directory mapping (must follow exactly): concept→wiki/concepts/, entity→wiki/entities/, synthesis→wiki/synthesis/, question→wiki/questions/, source→wiki/sources/, index→wiki/, overview→wiki/, log→wiki/

                Generate the wiki pages now:
                """.formatted(analysisJson, sourceText.length() > 8000 ? sourceText.substring(0, 8000) : sourceText,
                sourceSlug, today, title);
    }

    private void appendWikiLog(Path vaultRoot, String taskId, String title,
                                List<String> files, String today) {
        try {
            String existing = readSafe(vaultRoot, "wiki/log.md");
            String entry = "\n## " + today + " - Ingest " + taskId + "\n- Source: " + title
                    + "\n- Files: " + String.join(", ", files) + "\n";
            fileService.writeStringAtomically(vaultRoot, "wiki/log.md", existing + entry);
        } catch (Exception e) {
            log.warn("Failed to update wiki/log.md", e);
        }
    }

    private String slugFromPath(String path) {
        if (path == null) return "source";
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
