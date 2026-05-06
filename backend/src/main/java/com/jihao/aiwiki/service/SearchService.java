package com.jihao.aiwiki.service;

import com.jihao.aiwiki.domain.search.ContextBudgetService.ContextAssemblyResult;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;

import java.util.List;

/**
 * Vault 检索服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface SearchService {

    /**
     * 关键词搜索并组装 LLM 上下文。
     *
     * @param vaultId    Vault ID
     * @param query      关键词
     * @param maxTokens  最大 token 预算（≤0 使用默认值 6000）
     * @return 上下文组装结果
     */
    ContextAssemblyResult assembleContext(Long vaultId, String query, int maxTokens);

    /**
     * 关键词搜索，返回带摘要的结果列表（不含正文）。
     *
     * @param vaultId Vault ID
     * @param query   关键词
     * @return 搜索结果列表
     */
    List<WikiSearchResultVO> search(Long vaultId, String query);
}
