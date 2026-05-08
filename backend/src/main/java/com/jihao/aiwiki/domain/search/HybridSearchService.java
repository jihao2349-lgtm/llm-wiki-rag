package com.jihao.aiwiki.domain.search;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.domain.embedding.EmbeddingConfig;
import com.jihao.aiwiki.domain.embedding.EmbeddingException;
import com.jihao.aiwiki.domain.embedding.EmbeddingService;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索服务：向量 top20 + 关键词 top20 → RRF(k=60) 融合 → topK。
 *
 * <p>兜底逻辑：
 * <ul>
 *   <li>embedding_enabled=false → 纯关键词</li>
 *   <li>API Key 未配置 → 纯关键词</li>
 *   <li>无已向量化页面 → 纯关键词</li>
 *   <li>向量调用异常 → 退回纯关键词</li>
 * </ul>
 *
 * @author jihao
 * @date 2026/05/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int CANDIDATE_SIZE = 20;
    private static final int RRF_K = 60;

    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final KeywordSearchService keywordSearchService;
    private final WikiPageMapper wikiPageMapper;
    private final VaultProjectMapper vaultProjectMapper;
    private final VaultFileService vaultFileService;
    private final SettingService settingService;

    /**
     * 混合检索入口。
     *
     * @param vaultId Vault ID
     * @param query   用户查询文本
     * @param topK    最终返回数量
     * @return 按 RRF 分数降序排列的结果
     */
    public List<ScoredPage> search(Long vaultId, String query, int topK) {
        List<ScoredPage> keywordResults = keywordSearch(vaultId, query);

        EmbeddingConfig config = settingService.getEmbeddingConfig();
        if (!config.isEnabled() || !StringUtils.hasText(config.getApiKey())) {
            return keywordResults.stream().limit(topK).toList();
        }

        long successCount = wikiPageMapper.countByEmbedStatus(vaultId, "SUCCESS");
        if (successCount == 0) {
            log.debug("no embedded pages for vault {}, falling back to keyword", vaultId);
            return keywordResults.stream().limit(topK).toList();
        }

        try {
            float[] queryVec = embeddingService.embedQuery(query, vaultId);
            List<ScoredPage> vectorResults = vectorSearchService.search(vaultId, queryVec, CANDIDATE_SIZE);
            return rrfFusion(vectorResults, keywordResults, topK);
        } catch (EmbeddingException e) {
            log.warn("vector search failed, falling back to keyword: {}", e.getMessage());
            return keywordResults.stream().limit(topK).toList();
        }
    }

    /**
     * RRF 融合：score(doc) = Σ 1/(k + rank_i)，k=60。
     */
    private List<ScoredPage> rrfFusion(List<ScoredPage> vectorResults,
                                        List<ScoredPage> keywordResults,
                                        int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, ScoredPage> pageByPath = new HashMap<>();

        assignRrfScores(vectorResults, scores, pageByPath);
        assignRrfScores(keywordResults, scores, pageByPath);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    ScoredPage page = pageByPath.get(e.getKey());
                    page.setScore((int) (e.getValue() * 10000));
                    return page;
                })
                .toList();
    }

    private void assignRrfScores(List<ScoredPage> results,
                                  Map<String, Double> scores,
                                  Map<String, ScoredPage> pageByPath) {
        for (int i = 0; i < results.size(); i++) {
            ScoredPage page = results.get(i);
            String path = page.getPath();
            scores.merge(path, 1.0 / (RRF_K + i + 1), Double::sum);
            pageByPath.putIfAbsent(path, page);
        }
    }

    private List<ScoredPage> keywordSearch(Long vaultId, String query) {
        List<WikiPageDO> pages = wikiPageMapper.selectByVaultId(vaultId);
        Path vaultRoot = resolveVaultRoot(vaultId);

        List<ScoredPage> candidates = new ArrayList<>(pages.size());
        for (WikiPageDO p : pages) {
            String body = loadBody(vaultRoot, p.getPath());
            candidates.add(new ScoredPage(p.getPath(), p.getTitle(), 0, null, body, p.getType()));
        }
        return keywordSearchService.search(candidates, query);
    }

    private Path resolveVaultRoot(Long vaultId) {
        VaultProjectDO vault = vaultProjectMapper.selectById(vaultId);
        if (vault == null) return null;
        return Path.of(vault.getPath());
    }

    private String loadBody(Path vaultRoot, String relativePath) {
        if (vaultRoot == null) return null;
        try {
            return vaultFileService.readString(vaultRoot, relativePath);
        } catch (BusinessException e) {
            return null;
        }
    }
}
