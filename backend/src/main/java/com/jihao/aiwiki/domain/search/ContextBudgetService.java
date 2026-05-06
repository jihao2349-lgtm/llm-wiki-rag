package com.jihao.aiwiki.domain.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文预算服务，将检索到的页面按 token 预算组装为 LLM 上下文字符串。
 * 使用简单字符数估算：1 token ≈ 4 chars。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class ContextBudgetService {

    private static final int CHARS_PER_TOKEN = 4;
    private static final int DEFAULT_MAX_TOKENS = 6000;

    /**
     * 按评分顺序组装上下文，超出预算时截断。
     *
     * @param pages         已按评分排序的候选页面列表
     * @param maxTokens     最大 token 数，≤0 时使用默认值 6000
     * @return 组装结果
     */
    public ContextAssemblyResult assemble(List<ScoredPage> pages, int maxTokens) {
        int budget = (maxTokens <= 0 ? DEFAULT_MAX_TOKENS : maxTokens) * CHARS_PER_TOKEN;

        List<ScoredPage> included = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (ScoredPage page : pages) {
            String block = buildBlock(page);
            if (sb.length() + block.length() > budget) {
                break;
            }
            sb.append(block);
            included.add(page);
        }
        return new ContextAssemblyResult(sb.toString(), included);
    }

    private String buildBlock(ScoredPage page) {
        return "## " + page.getTitle() + "\npath: " + page.getPath() + "\n\n"
                + (page.getBody() != null ? page.getBody() : "") + "\n\n---\n\n";
    }

    /**
     * 上下文组装结果。
     */
    public record ContextAssemblyResult(String context, List<ScoredPage> includedPages) {}
}
