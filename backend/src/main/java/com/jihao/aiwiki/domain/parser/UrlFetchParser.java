package com.jihao.aiwiki.domain.parser;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * URL 抓取解析器，使用 Jsoup 抓取网页并提取正文，含 SSRF 防护。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class UrlFetchParser implements DocumentParser {

    private static final int TIMEOUT_MS = 10_000;
    private static final int MAX_BODY_SIZE = 2 * 1024 * 1024; // 2 MB

    @Override
    public boolean supports(String filename) {
        if (filename == null) return false;
        return filename.startsWith("http://") || filename.startsWith("https://");
    }

    @Override
    public String extractText(InputStream input, String filename) throws IOException {
        return fetch(filename);
    }

    /**
     * 抓取 URL 并提取文本，filename 参数作为 URL 传入。
     *
     * @param url 目标 URL
     * @return 标题 + 正文文本
     */
    public String fetch(String url) throws IOException {
        validateSsrf(url);
        Document doc = Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .maxBodySize(MAX_BODY_SIZE)
                .followRedirects(true)
                // 模拟真实浏览器，避免被反爬虫 403 拦截
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .get();
        String title = doc.title();
        String body = doc.body().text();
        return title.isBlank() ? body : title + "\n\n" + body;
    }

    private void validateSsrf(String rawUrl) {
        URL url;
        try {
            url = new URL(rawUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.SOURCE_URL_INVALID, "invalid url: " + rawUrl);
        }
        String scheme = url.getProtocol();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new BusinessException(ErrorCode.SOURCE_URL_INVALID, "only http/https allowed");
        }
        String host = url.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.SOURCE_URL_INVALID, "empty host");
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            if (isPrivateAddress(addr)) {
                throw new BusinessException(ErrorCode.SOURCE_URL_SSRF, "ssrf blocked: private address");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SOURCE_URL_INVALID, "cannot resolve host: " + host);
        }
    }

    private boolean isPrivateAddress(InetAddress addr) {
        return addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress();
    }
}
