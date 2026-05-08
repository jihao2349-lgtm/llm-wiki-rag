package com.jihao.aiwiki.domain.search;

import com.jihao.aiwiki.mapper.WikiPageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MySQL 8.4 VECTOR 向量检索封装。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Component
@RequiredArgsConstructor
public class VectorSearchService {

    private final WikiPageMapper wikiPageMapper;

    /**
     * 向量近似检索，返回按距离升序排列的结果（distance 越小越相似）。
     *
     * @param vaultId      Vault ID
     * @param queryVec     查询向量
     * @param topK         返回数量
     * @return 结果列表，vectorDistance 字段已填充
     */
    public List<ScoredPage> search(Long vaultId, float[] queryVec, int topK) {
        String vecJson = floatArrayToJson(queryVec);
        return wikiPageMapper.vectorSearch(vaultId, vecJson, topK);
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
}
