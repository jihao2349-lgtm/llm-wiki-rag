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

    private ScoredPage page(String path, String title, String body) {
        return new ScoredPage(path, title, 0, null, body, "concept");
    }
}
