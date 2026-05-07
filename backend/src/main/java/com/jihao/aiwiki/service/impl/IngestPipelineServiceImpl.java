package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.domain.ingest.pipeline.IngestPipeline;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskHandler;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskRunContext;
import com.jihao.aiwiki.domain.ingest.queue.IngestTaskStage;
import com.jihao.aiwiki.domain.llm.LlmChatRequest;
import com.jihao.aiwiki.domain.vault.SecretCipher;
import com.jihao.aiwiki.entity.AppSettingDO;
import com.jihao.aiwiki.entity.IngestTaskDO;
import com.jihao.aiwiki.entity.SourceDocumentDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.AppSettingMapper;
import com.jihao.aiwiki.mapper.SourceDocumentMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.service.WikiPageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 摄入流水线 IngestTaskHandler 实现。
 * 由 T3 队列框架在任务执行时调用。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
public class IngestPipelineServiceImpl implements IngestTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(IngestPipelineServiceImpl.class);

    private static final String KEY_PROVIDER = "llm.provider";
    private static final String KEY_BASE_URL = "llm.base_url";
    private static final String KEY_API_KEY_CIPHER = "llm.api_key_cipher";
    private static final String KEY_MODEL = "llm.model";
    private static final String KEY_TEMPERATURE = "llm.temperature";
    private static final String KEY_OUTPUT_LANGUAGE = "llm.output_language";

    private static final List<String> SETTING_KEYS = List.of(
            KEY_PROVIDER, KEY_BASE_URL, KEY_API_KEY_CIPHER, KEY_MODEL, KEY_TEMPERATURE, KEY_OUTPUT_LANGUAGE
    );

    private final IngestPipeline ingestPipeline;
    private final SourceDocumentMapper sourceMapper;
    private final VaultProjectMapper vaultMapper;
    private final AppSettingMapper appSettingMapper;
    private final SecretCipher secretCipher;
    private final WikiPageService wikiPageService;

    public IngestPipelineServiceImpl(IngestPipeline ingestPipeline,
                                     SourceDocumentMapper sourceMapper,
                                     VaultProjectMapper vaultMapper,
                                     AppSettingMapper appSettingMapper,
                                     SecretCipher secretCipher,
                                     WikiPageService wikiPageService) {
        this.ingestPipeline = ingestPipeline;
        this.sourceMapper = sourceMapper;
        this.vaultMapper = vaultMapper;
        this.appSettingMapper = appSettingMapper;
        this.secretCipher = secretCipher;
        this.wikiPageService = wikiPageService;
    }

    @Override
    public void handle(IngestTaskRunContext context) throws Exception {
        IngestTaskDO task = context.getTask();
        log.info("IngestPipeline starting taskId={} sourceId={} vaultId={}", task.getTaskId(), task.getSourceId(), task.getVaultId());

        // ---- Load source ----
        context.updateProgress(IngestTaskStage.PARSING, 5);
        SourceDocumentDO source = sourceMapper.selectById(task.getSourceId());
        if (source == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND,
                    "source not found: " + task.getSourceId());
        }

        // ---- Load vault ----
        VaultProjectDO vault = vaultMapper.selectById(task.getVaultId());
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED,
                    "vault not found: " + task.getVaultId());
        }

        // ---- Build LLM request template ----
        Map<String, String> settings = loadSettings();
        LlmChatRequest llmTemplate;
        try {
            llmTemplate = buildLlmTemplate(settings);
        } catch (Exception e) {
            log.error("IngestPipeline taskId={} failed to build LLM template: {}", task.getTaskId(), e.getMessage());
            throw e;
        }

        String outputLanguage = firstText(settings.get(KEY_OUTPUT_LANGUAGE), "follow source language");

        // ---- Run two-phase pipeline ----
        try {
            ingestPipeline.run(context, vault, source, llmTemplate, outputLanguage);
        } catch (Exception e) {
            log.error("IngestPipeline taskId={} failed at stage pipeline.run: {}", task.getTaskId(), e.getMessage());
            throw e;
        }

        // ---- Reindex wiki_page after writing ----
        context.updateProgress(IngestTaskStage.INDEXING, 92);
        wikiPageService.reindex(vault.getId(), vault.getPath());

        context.updateProgress(IngestTaskStage.DONE, 100);
        log.info("IngestPipeline completed taskId={}", task.getTaskId());
    }

    private LlmChatRequest buildLlmTemplate(Map<String, String> settings) {
        String cipher = settings.get(KEY_API_KEY_CIPHER);
        String apiKey = StringUtils.hasText(cipher) ? secretCipher.decrypt(cipher) : null;
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ErrorCode.LLM_CALL_FAILED, "LLM API key not configured");
        }
        BigDecimal temperature = parseDecimal(settings.get(KEY_TEMPERATURE), BigDecimal.valueOf(0.3));
        return LlmChatRequest.builder()
                .provider(firstText(settings.get(KEY_PROVIDER), "openai"))
                .baseUrl(firstText(settings.get(KEY_BASE_URL), "https://api.openai.com/v1"))
                .apiKey(apiKey)
                .model(firstText(settings.get(KEY_MODEL), "gpt-4o-mini"))
                .temperature(temperature)
                .build();
    }

    private Map<String, String> loadSettings() {
        return appSettingMapper.findByKeys(SETTING_KEYS).stream()
                .filter(s -> StringUtils.hasText(s.getSettingValue()))
                .collect(Collectors.toMap(AppSettingDO::getSettingKey, AppSettingDO::getSettingValue));
    }

    private String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) return v.trim();
        }
        return "";
    }

    private BigDecimal parseDecimal(String value, BigDecimal defaultValue) {
        try {
            return StringUtils.hasText(value) ? new BigDecimal(value) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
