package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.domain.search.ContextBudgetService;
import com.jihao.aiwiki.domain.search.ContextBudgetService.ContextAssemblyResult;
import com.jihao.aiwiki.domain.search.HybridSearchService;
import com.jihao.aiwiki.domain.search.ScoredPage;
import com.jihao.aiwiki.service.SearchService;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vault 检索服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final ContextBudgetService contextBudgetService;
    private final HybridSearchService hybridSearchService;

    public SearchServiceImpl(ContextBudgetService contextBudgetService,
                             HybridSearchService hybridSearchService) {
        this.contextBudgetService = contextBudgetService;
        this.hybridSearchService = hybridSearchService;
    }

    @Override
    public ContextAssemblyResult assembleContext(Long vaultId, String query, int maxTokens) {
        List<ScoredPage> results = hybridSearchService.search(vaultId, query, 20);
        return contextBudgetService.assemble(results, maxTokens);
    }

    @Override
    public List<WikiSearchResultVO> search(Long vaultId, String query) {
        List<ScoredPage> results = hybridSearchService.search(vaultId, query, 20);
        return results.stream().map(r -> WikiSearchResultVO.builder()
                .path(r.getPath())
                .title(r.getTitle())
                .type(r.getType())
                .score(r.getScore())
                .snippet(r.getSnippet())
                .build()).toList();
    }
}
