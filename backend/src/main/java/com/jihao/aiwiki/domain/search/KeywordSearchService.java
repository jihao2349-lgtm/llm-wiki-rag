package com.jihao.aiwiki.domain.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 关键词搜索评分服务，对 Wiki 页面按关键词相关度打分并排序。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class KeywordSearchService {

    /**
     * 对候选页面列表按关键词评分并排序，过滤零分结果。
     *
     * @param pages 候选页面列表（含 path、title、body、type）
     * @param query 搜索关键词，支持空格分隔多词
     * @return 按分数降序排列的结果列表（score &gt; 0）
     */
    public List<ScoredPage> search(List<ScoredPage> pages, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] words = query.toLowerCase().split("[\\s，,。.！!？?]+");

        List<ScoredPage> results = new ArrayList<>();
        for (ScoredPage page : pages) {
            int score = calcScore(page, words, query.toLowerCase());
            if (score > 0) {
                page.setScore(score);
                page.setSnippet(extractSnippet(page.getBody(), words));
                results.add(page);
            }
        }
        results.sort(Comparator.comparingInt(ScoredPage::getScore).reversed());
        return results;
    }

    /**
     * 计算单个页面的相关分数。
     *
     * @param page  候选页面
     * @param words 关键词拆分后的词组
     * @param fullQuery 完整查询字符串（小写）
     * @return 分数，0 表示不相关
     */
    private int calcScore(ScoredPage page, String[] words, String fullQuery) {
        int score = 0;
        String titleLower = page.getTitle() == null ? "" : page.getTitle().toLowerCase();
        String bodyLower = page.getBody() == null ? "" : page.getBody().toLowerCase();

        // 标题完全匹配
        if (titleLower.equals(fullQuery)) {
            score += 100;
        }

        for (String word : words) {
            if (word.isEmpty()) continue;
            // 标题包含词
            if (titleLower.contains(word)) score += 50;
            // 文件名包含词
            String filename = page.getPath();
            int slash = filename.lastIndexOf('/');
            if (slash >= 0) filename = filename.substring(slash + 1);
            if (filename.replace(".md", "").toLowerCase().contains(word)) score += 40;
            // 正文关键词计数（每次 +10，上限 +100）
            int count = 0;
            int idx = 0;
            while ((idx = bodyLower.indexOf(word, idx)) >= 0) {
                count++;
                idx += word.length();
            }
            score += Math.min(count * 10, 100);
        }
        return score;
    }

    /**
     * 从正文中提取含关键词的摘要片段。
     *
     * @param body  正文内容
     * @param words 关键词数组
     * @return 摘要片段（最长 200 字符）
     */
    private String extractSnippet(String body, String[] words) {
        if (body == null || body.isEmpty()) return "";
        String lower = body.toLowerCase();
        for (String word : words) {
            int idx = lower.indexOf(word);
            if (idx >= 0) {
                int start = Math.max(0, idx - 60);
                int end = Math.min(body.length(), idx + 140);
                return (start > 0 ? "…" : "") + body.substring(start, end) + (end < body.length() ? "…" : "");
            }
        }
        return body.length() > 200 ? body.substring(0, 200) + "…" : body;
    }
}
