package com.jihao.aiwiki.domain.search;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 关键词搜索评分服务，对 Wiki 页面按关键词相关度打分并排序。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class KeywordSearchService {

    /** 中文停用词：高频功能词/疑问词，无实际检索价值，跳过评分 */
    private static final Set<String> STOPWORDS = Set.of(
            "是", "什么", "是什么", "的", "了", "吗", "呢", "吧", "啊",
            "在", "有", "和", "与", "或", "这", "那", "它", "我", "你", "他", "她",
            "们", "一", "个", "怎么", "如何", "为什么", "哪些", "哪个", "么", "嘛",
            "这个", "那个", "一个", "可以", "能", "会", "请问", "告诉", "介绍"
    );

    /**
     * 将查询字符串分词，支持 CJK bigram 展开。
     * 对含 CJK 字符且长度大于 2 的词：生成重叠 bigram + 单字（非停用词）+ 原词，
     * 其余词直接保留。停用词整体跳过。
     */
    String[] tokenize(String query) {
        String[] parts = query.toLowerCase().split("[\\s，,。.！!？?]+");
        Set<String> tokens = new LinkedHashSet<>();
        for (String word : parts) {
            if (word.isEmpty() || STOPWORDS.contains(word)) continue;
            if (hasCjk(word) && word.length() > 2) {
                // bigrams: "连接池" → ["连接", "接池"]
                for (int i = 0; i < word.length() - 1; i++) {
                    tokens.add(word.substring(i, i + 2));
                }
                // individual chars (non-stopword)
                for (int i = 0; i < word.length(); i++) {
                    String ch = word.substring(i, i + 1);
                    if (!STOPWORDS.contains(ch)) tokens.add(ch);
                }
                tokens.add(word);
            } else {
                tokens.add(word);
            }
        }
        return tokens.toArray(new String[0]);
    }

    private boolean hasCjk(String s) {
        return s.chars().anyMatch(c -> (c >= 0x4e00 && c <= 0x9fff) || (c >= 0x3400 && c <= 0x4dbf));
    }

    /**
     * 对候选页面列表按关键词评分并排序，过滤零分结果。
     *
     * @param pages 候选页面列表（含 path、title、body、type）
     * @param query 搜索关键词，支持空格分隔多词及中文连续串
     * @return 按分数降序排列的结果列表（score &gt; 0）
     */
    public List<ScoredPage> search(List<ScoredPage> pages, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String[] words = tokenize(query);

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
     * 计算单个页面的相关分数。词列表已由 tokenize 完成停用词过滤和 CJK bigram 展开。
     */
    private int calcScore(ScoredPage page, String[] words, String fullQuery) {
        int score = 0;
        String titleLower = page.getTitle() == null ? "" : page.getTitle().toLowerCase();
        String bodyLower = page.getBody() == null ? "" : page.getBody().toLowerCase();

        // 标题完全匹配（用原始 query）
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
     */
    private String extractSnippet(String body, String[] words) {
        if (body == null || body.isEmpty()) return "";
        String lower = body.toLowerCase();
        for (String word : words) {
            if (word.isEmpty()) continue;
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
