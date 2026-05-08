package com.jihao.aiwiki.domain.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Embedding 业务服务：单页同步向量化、批量异步向量化、query 向量化、统计。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private static final String PROGRESS_KEY = "embedding:progress:";
    private static final String LOCK_KEY = "embedding:lock:";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);
    private static final Duration PROGRESS_TTL = Duration.ofHours(1);

    private final EmbeddingClient embeddingClient;
    private final EmbeddingTextBuilder textBuilder;
    private final WikiPageMapper wikiPageMapper;
    private final SettingService settingService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 同步向量化单个页面。
     * 若 hash 未变化则跳过，返回 skipped=true。
     */
    public EmbeddingResult embedPage(Long pageId) {
        WikiPageDO page = wikiPageMapper.selectById(pageId);
        if (page == null) {
            return EmbeddingResult.failed(pageId, "page not found");
        }
        EmbeddingConfig config = settingService.getEmbeddingConfig();
        if (!config.isEnabled()) {
            return EmbeddingResult.failed(pageId, "embedding disabled");
        }
        if (!StringUtils.hasText(config.getApiKey())) {
            return EmbeddingResult.failed(pageId, "embedding API key not configured");
        }

        String newHash = textBuilder.contentHash(page);
        if (newHash.equals(page.getEmbedContentHash()) && "SUCCESS".equals(page.getEmbedStatus())) {
            return EmbeddingResult.skipped(pageId);
        }

        try {
            String text = textBuilder.build(page);
            float[] vec = embeddingClient.embedSingle(text, config);
            String vecJson = floatArrayToJson(vec);
            wikiPageMapper.updateEmbeddingSuccess(pageId, vecJson, config.getModel(), newHash, LocalDateTime.now());
            log.info("embedded page {} ({})", pageId, page.getPath());
            return EmbeddingResult.success(pageId);
        } catch (EmbeddingException e) {
            String error = truncate(e.getMessage(), 480);
            wikiPageMapper.updateEmbeddingFailed(pageId, error);
            log.warn("embed failed for page {}: {}", pageId, error);
            return EmbeddingResult.failed(pageId, error);
        }
    }

    /**
     * 批量异步向量化。分批调用，实时更新 Redis 进度。
     * 同一 vault 同时只允许一个任务（Redis SETNX 锁）。
     */
    @Async("embeddingExecutor")
    public void embedPagesBatch(List<Long> pageIds, Long vaultId) {
        String lockKey = LOCK_KEY + vaultId;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("embedding batch for vault {} already running, skip", vaultId);
            return;
        }
        try {
            EmbeddingConfig config = settingService.getEmbeddingConfig();
            if (!config.isEnabled() || !StringUtils.hasText(config.getApiKey())) {
                log.warn("embedding batch aborted: disabled or no API key");
                return;
            }
            int total = pageIds.size();
            int current = 0;
            setProgress(vaultId, true, current, total);

            int batchSize = config.getBatchSize();
            for (int i = 0; i < pageIds.size(); i += batchSize) {
                List<Long> batch = pageIds.subList(i, Math.min(i + batchSize, pageIds.size()));
                processBatch(batch, config);
                current += batch.size();
                setProgress(vaultId, true, current, total);
            }
            log.info("embedding batch complete for vault {}: {}/{}", vaultId, current, total);
        } finally {
            setProgress(vaultId, false, 0, 0);
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * query 向量化（同步，无 hash 检查）。
     */
    public float[] embedQuery(String query, Long vaultId) {
        EmbeddingConfig config = settingService.getEmbeddingConfig();
        if (!config.isEnabled() || !StringUtils.hasText(config.getApiKey())) {
            throw new EmbeddingException("embedding not configured");
        }
        return embeddingClient.embedSingle(query, config);
    }

    /**
     * 统计 vault 向量化状态。
     */
    public EmbeddingStats stats(Long vaultId) {
        long total = wikiPageMapper.countByVaultId(vaultId);
        long success = wikiPageMapper.countByEmbedStatus(vaultId, "SUCCESS");
        long failed = wikiPageMapper.countByEmbedStatus(vaultId, "FAILED");
        long pending = total - success - failed;
        LocalDateTime lastEmbeddedAt = wikiPageMapper.selectLastEmbeddedAt(vaultId);
        return EmbeddingStats.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .pending(Math.max(pending, 0))
                .lastEmbeddedAt(lastEmbeddedAt)
                .build();
    }

    /**
     * 读取 Redis 进度。
     */
    public EmbeddingProgress getProgress(Long vaultId) {
        String json = redisTemplate.opsForValue().get(PROGRESS_KEY + vaultId);
        if (json == null) {
            return EmbeddingProgress.idle();
        }
        try {
            return objectMapper.readValue(json, EmbeddingProgress.class);
        } catch (Exception e) {
            return EmbeddingProgress.idle();
        }
    }

    /** 是否有任务在跑（Redis 锁存在）。 */
    public boolean isRunning(Long vaultId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY + vaultId));
    }

    // ---- private helpers ----

    private void processBatch(List<Long> ids, EmbeddingConfig config) {
        List<WikiPageDO> pages = ids.stream()
                .map(wikiPageMapper::selectById)
                .filter(p -> p != null)
                .toList();

        List<String> texts = new ArrayList<>();
        List<WikiPageDO> toEmbed = new ArrayList<>();
        for (WikiPageDO page : pages) {
            String newHash = textBuilder.contentHash(page);
            if (newHash.equals(page.getEmbedContentHash()) && "SUCCESS".equals(page.getEmbedStatus())) {
                continue;
            }
            texts.add(textBuilder.build(page));
            toEmbed.add(page);
        }
        if (toEmbed.isEmpty()) return;

        try {
            List<float[]> vecs = embeddingClient.embed(texts, config);
            for (int i = 0; i < toEmbed.size(); i++) {
                WikiPageDO page = toEmbed.get(i);
                String vecJson = floatArrayToJson(vecs.get(i));
                String hash = textBuilder.contentHash(page);
                wikiPageMapper.updateEmbeddingSuccess(page.getId(), vecJson, config.getModel(), hash, LocalDateTime.now());
            }
        } catch (EmbeddingException e) {
            String error = truncate(e.getMessage(), 480);
            for (WikiPageDO page : toEmbed) {
                wikiPageMapper.updateEmbeddingFailed(page.getId(), error);
            }
            log.warn("batch embed failed: {}", error);
        }
    }

    private void setProgress(Long vaultId, boolean processing, int current, int total) {
        try {
            EmbeddingProgress progress = processing
                    ? EmbeddingProgress.running(current, total)
                    : EmbeddingProgress.idle();
            String json = objectMapper.writeValueAsString(progress);
            redisTemplate.opsForValue().set(PROGRESS_KEY + vaultId, json, PROGRESS_TTL);
        } catch (Exception e) {
            log.warn("failed to update embedding progress in redis", e);
        }
    }

    private String floatArrayToJson(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vec[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }
}
