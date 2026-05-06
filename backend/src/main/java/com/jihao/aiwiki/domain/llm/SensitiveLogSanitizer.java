package com.jihao.aiwiki.domain.llm;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 日志敏感字段脱敏工具。
 *
 * @author jihao
 * @date 2026/05/06
 */
public final class SensitiveLogSanitizer {

    /** Authorization header pattern */
    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile("(?i)(Authorization\\s*[:=]\\s*Bearer\\s+)[^\\s,}]+");

    /** API key json pattern */
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(\"?api[_-]?key\"?\\s*[:=]\\s*\")([^\"]+)(\")");

    /**
     * Utility class.
     */
    private SensitiveLogSanitizer() {
    }

    /**
     * 对日志文本做敏感字段脱敏。
     *
     * @param value 原始文本
     * @return 脱敏文本
     */
    public static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String sanitized = AUTHORIZATION_PATTERN.matcher(value).replaceAll("$1****");
        return API_KEY_PATTERN.matcher(sanitized).replaceAll("$1****$3");
    }
}
