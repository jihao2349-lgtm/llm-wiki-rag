package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.domain.search.ContextBudgetService;
import com.jihao.aiwiki.domain.search.ContextBudgetService.ContextAssemblyResult;
import com.jihao.aiwiki.domain.search.KeywordSearchService;
import com.jihao.aiwiki.domain.search.ScoredPage;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.SearchService;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Vault 检索服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final WikiPageMapper wikiPageMapper;
    private final VaultProjectMapper vaultMapper;
    private final VaultFileService fileService;
    private final KeywordSearchService keywordSearchService;
    private final ContextBudgetService contextBudgetService;

    public SearchServiceImpl(WikiPageMapper wikiPageMapper,
                              VaultProjectMapper vaultMapper,
                              VaultFileService fileService,
                              KeywordSearchService keywordSearchService,
                              ContextBudgetService contextBudgetService) {
        this.wikiPageMapper = wikiPageMapper;
        this.vaultMapper = vaultMapper;
        this.fileService = fileService;
        this.keywordSearchService = keywordSearchService;
        this.contextBudgetService = contextBudgetService;
    }

    @Override
    public ContextAssemblyResult assembleContext(Long vaultId, String query, int maxTokens) {
        List<ScoredPage> results = buildScoredPages(vaultId, query);
        return contextBudgetService.assemble(results, maxTokens);
    }

    @Override
    public List<WikiSearchResultVO> search(Long vaultId, String query) {
        List<ScoredPage> results = buildScoredPages(vaultId, query);
        return results.stream().map(r -> WikiSearchResultVO.builder()
                .path(r.getPath())
                .title(r.getTitle())
                .type(r.getType())
                .score(r.getScore())
                .snippet(r.getSnippet())
                .build()).toList();
    }

    private List<ScoredPage> buildScoredPages(Long vaultId, String query) {
        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) return List.of();

        Path vaultRoot = Path.of(vault.getPath());
        List<WikiPageDO> pages = wikiPageMapper.selectByVaultId(vaultId);

        List<ScoredPage> candidates = pages.stream().map(p -> {
            String body = readBodySafe(vaultRoot, p.getPath());
            return new ScoredPage(p.getPath(), p.getTitle(), 0, null, body, p.getType());
        }).toList();

        return keywordSearchService.search(candidates, query);
    }

    private String readBodySafe(Path vaultRoot, String path) {
        try {
            return fileService.readString(vaultRoot, path);
        } catch (BusinessException e) {
            return null;
        }
    }
}
