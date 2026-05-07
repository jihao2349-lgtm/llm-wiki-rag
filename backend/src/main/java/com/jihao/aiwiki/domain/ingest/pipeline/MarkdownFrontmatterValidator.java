package com.jihao.aiwiki.domain.ingest.pipeline;

import org.springframework.stereotype.Component;

/**
 * Markdown frontmatter 校验器，用于 FILE block 写入前验证。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class MarkdownFrontmatterValidator {

    /**
     * 校验 FILE block 的路径安全性。
     *
     * @param path FILE block 声明的路径
     * @return 校验失败原因，通过时返回 null
     */
    public String validatePath(String path) {
        if (path == null || path.isBlank()) return "path is empty";
        if (!path.startsWith("wiki/")) return "path must start with wiki/";
        if (path.contains("..")) return "path contains ..";
        if (path.contains("\\")) return "path contains backslash";
        if (path.contains("\0")) return "path contains null byte";
        if (path.matches(".*[A-Z]:\\\\.*")) return "path contains windows drive letter";
        return null;
    }

    /**
     * 校验 Markdown frontmatter 必填字段。
     *
     * @param content Markdown 全文（含 frontmatter）
     * @return 校验失败原因，通过时返回 null
     */
    public String validateFrontmatter(String content) {
        if (content == null || !content.startsWith("---")) {
            return "missing frontmatter";
        }
        int end = content.indexOf("\n---", 3);
        if (end < 0) return "frontmatter not closed";
        String fm = content.substring(3, end);

        if (!containsField(fm, "title")) return "frontmatter missing field: title";
        if (!containsField(fm, "type")) return "frontmatter missing field: type";
        if (!containsField(fm, "sources")) return "frontmatter missing field: sources";
        if (!containsField(fm, "updated")) return "frontmatter missing field: updated";

        return null;
    }

    private boolean containsField(String fm, String field) {
        return fm.contains("\n" + field + ":") || fm.startsWith(field + ":");
    }


}
