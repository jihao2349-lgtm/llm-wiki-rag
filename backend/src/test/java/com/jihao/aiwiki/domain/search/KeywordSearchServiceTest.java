package com.jihao.aiwiki.domain.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordSearchServiceTest {

    private final KeywordSearchService service = new KeywordSearchService();

    @Test
    void search_titleExactMatch_highestScore() {
        var pages = List.of(
                page("wiki/a.md", "agent memory", "some body text"),
                page("wiki/b.md", "unrelated page", "nothing here")
        );
        var results = service.search(pages, "agent memory");

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getTitle()).isEqualTo("agent memory");
        assertThat(results.get(0).getScore()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void search_emptyQuery_returnsEmpty() {
        var pages = List.of(page("wiki/a.md", "title", "body"));
        assertThat(service.search(pages, "")).isEmpty();
        assertThat(service.search(pages, null)).isEmpty();
        assertThat(service.search(pages, "   ")).isEmpty();
    }

    @Test
    void search_zeroScoreFiltered() {
        var pages = List.of(page("wiki/a.md", "unrelated", "no match here"));
        var results = service.search(pages, "xyznotfound");
        assertThat(results).isEmpty();
    }

    @Test
    void search_bodyFrequency_capped() {
        String body = "keyword ".repeat(20);
        var pages = List.of(page("wiki/a.md", "title", body));
        var results = service.search(pages, "keyword");

        assertThat(results).hasSize(1);
        // body capped at 100, plus title contains (+50) = at most 150 + title exact may not match
        assertThat(results.get(0).getScore()).isLessThanOrEqualTo(200);
    }

    @Test
    void search_sortedByScoreDescending() {
        var pages = List.of(
                page("wiki/a.md", "java basics", "intro to java programming"),
                page("wiki/b.md", "java", "java java java java java java java java java java java java")
        );
        var results = service.search(pages, "java");
        assertThat(results.get(0).getScore()).isGreaterThanOrEqualTo(results.get(1).getScore());
    }

    @Test
    void search_snippetContainsKeyword() {
        var pages = List.of(page("wiki/a.md", "title", "this body has the keyword somewhere in it"));
        var results = service.search(pages, "keyword");
        assertThat(results.get(0).getSnippet()).contains("keyword");
    }

    // ---- CJK bigram tests ----

    @Test
    void tokenize_chineseNoSpaces_producesBigrams() {
        String[] tokens = service.tokenize("连接池配置");
        // should contain bigrams "连接", "接池", "池配", "配置"
        assertThat(tokens).contains("连接", "接池", "池配", "配置");
    }

    @Test
    void tokenize_stopwordFiltered() {
        String[] tokens = service.tokenize("是什么");
        assertThat(tokens).isEmpty();
    }

    @Test
    void tokenize_mixedQueryWithSpace() {
        String[] tokens = service.tokenize("Hermes 是什么");
        // "是什么" is stopword, only "hermes" survives
        assertThat(tokens).contains("hermes");
        assertThat(tokens).doesNotContain("是什么");
    }

    @Test
    void search_chineseQueryNoSpaces_matchesBodyByBigram() {
        // body contains "连接池" but not the full phrase "连接池配置"
        var pages = List.of(page("wiki/a.md", "数据库", "连接池的配置方法"));
        var results = service.search(pages, "连接池配置");
        assertThat(results).isNotEmpty();
    }

    @Test
    void search_chineseQueryNoSpaces_noFalsePositive() {
        var pages = List.of(page("wiki/a.md", "网络协议", "完全无关的内容"));
        var results = service.search(pages, "连接池配置");
        assertThat(results).isEmpty();
    }

    private ScoredPage page(String path, String title, String body) {
        return new ScoredPage(path, title, 0, null, body, "concept");
    }
}
