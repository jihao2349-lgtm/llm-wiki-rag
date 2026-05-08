package com.jihao.aiwiki.domain.embedding;

import com.jihao.aiwiki.domain.search.MarkdownFrontmatterParser;
import com.jihao.aiwiki.domain.search.ParsedFrontmatter;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 从 WikiPageDO 构造向量化文本并计算内容 hash。
 * 规则：title + "\n" + body 前 1500 字，总长 <= 2000 字符。
 *
 * @author jihao
 * @date 2026/05/08
 */
@Component
public class EmbeddingTextBuilder {

    private static final int BODY_LIMIT = 1500;
    private static final int TOTAL_LIMIT = 2000;

    private final VaultProjectMapper vaultProjectMapper;
    private final VaultFileService vaultFileService;
    private final MarkdownFrontmatterParser frontmatterParser;

    public EmbeddingTextBuilder(VaultProjectMapper vaultProjectMapper,
                                VaultFileService vaultFileService,
                                MarkdownFrontmatterParser frontmatterParser) {
        this.vaultProjectMapper = vaultProjectMapper;
        this.vaultFileService = vaultFileService;
        this.frontmatterParser = frontmatterParser;
    }

    /**
     * 构造向量化文本：title + body 前 1500 字，总不超过 2000 字符。
     */
    public String build(WikiPageDO page) {
        String rawContent = loadRawContent(page);
        ParsedFrontmatter fm = frontmatterParser.parse(rawContent != null ? rawContent : "");
        String body = fm.getBody() != null ? fm.getBody() : (rawContent != null ? rawContent : "");

        StringBuilder sb = new StringBuilder();
        sb.append(page.getTitle() != null ? page.getTitle() : "");
        sb.append("\n");
        sb.append(body, 0, Math.min(body.length(), BODY_LIMIT));

        String text = sb.toString();
        return text.length() > TOTAL_LIMIT ? text.substring(0, TOTAL_LIMIT) : text;
    }

    /**
     * SHA-256(title + body前1500字)，用于增量判断。
     */
    public String contentHash(WikiPageDO page) {
        String rawContent = loadRawContent(page);
        String body = rawContent != null ? rawContent : "";
        String bodySlice = body.length() > BODY_LIMIT ? body.substring(0, BODY_LIMIT) : body;
        String content = (page.getTitle() != null ? page.getTitle() : "") + bodySlice;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    private String loadRawContent(WikiPageDO page) {
        try {
            VaultProjectDO vault = vaultProjectMapper.selectById(page.getVaultId());
            if (vault == null) return null;
            return vaultFileService.readString(Path.of(vault.getPath()), page.getPath());
        } catch (Exception e) {
            return null;
        }
    }
}
