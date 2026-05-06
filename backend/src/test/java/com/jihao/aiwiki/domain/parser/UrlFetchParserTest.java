package com.jihao.aiwiki.domain.parser;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlFetchParserTest {

    private final UrlFetchParser parser = new UrlFetchParser();

    @Test
    void supports_httpUrls() {
        assertThat(parser.supports("http://example.com")).isTrue();
        assertThat(parser.supports("https://example.com")).isTrue();
        assertThat(parser.supports("page.html")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void fetch_localhost_ssrfBlocked() {
        assertThatThrownBy(() -> parser.fetch("http://localhost/api/secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ssrf");
    }

    @Test
    void fetch_loopback_ssrfBlocked() {
        assertThatThrownBy(() -> parser.fetch("http://127.0.0.1/secret"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SOURCE_URL_SSRF);
                });
    }

    @Test
    void fetch_privateSubnet_ssrfBlocked() {
        assertThatThrownBy(() -> parser.fetch("http://192.168.1.1/admin"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SOURCE_URL_SSRF);
                });
    }

    @Test
    void fetch_fileScheme_rejected() {
        assertThatThrownBy(() -> parser.fetch("file:///etc/passwd"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void fetch_invalidUrl_rejected() {
        assertThatThrownBy(() -> parser.fetch("not-a-url"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SOURCE_URL_INVALID);
                });
    }
}
