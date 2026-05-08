package com.jihao.aiwiki.domain.search;

import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 应用层向量检索：从 DB 加载所有已向量化页面，计算余弦相似度，返回 top-K。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Component
@RequiredArgsConstructor
public class VectorSearchService {

    private final WikiPageMapper wikiPageMapper;

    public List<ScoredPage> search(Long vaultId, float[] queryVec, int topK) {
        List<WikiPageDO> rows = wikiPageMapper.selectSuccessEmbeddings(vaultId);
        List<ScoredPage> results = new ArrayList<>(rows.size());

        for (WikiPageDO row : rows) {
            if (row.getEmbedding() == null) continue;
            float[] docVec = parseJson(row.getEmbedding());
            if (docVec == null || docVec.length != queryVec.length) continue;

            double similarity = cosineSimilarity(queryVec, docVec);
            ScoredPage sp = new ScoredPage();
            sp.setPageId(row.getId());
            sp.setPath(row.getPath());
            sp.setTitle(row.getTitle());
            sp.setType(row.getType());
            sp.setVectorDistance(1.0 - similarity);
            results.add(sp);
        }

        results.sort(Comparator.comparingDouble(ScoredPage::getVectorDistance));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private float[] parseJson(String json) {
        try {
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) trimmed = trimmed.substring(1);
            if (trimmed.endsWith("]")) trimmed = trimmed.substring(0, trimmed.length() - 1);
            String[] parts = trimmed.split(",");
            float[] vec = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vec[i] = Float.parseFloat(parts[i].trim());
            }
            return vec;
        } catch (Exception e) {
            return null;
        }
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }
}
