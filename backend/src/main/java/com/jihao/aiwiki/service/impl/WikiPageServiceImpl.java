package com.jihao.aiwiki.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.domain.search.KeywordSearchService;
import com.jihao.aiwiki.domain.search.MarkdownFrontmatterParser;
import com.jihao.aiwiki.domain.search.ParsedFrontmatter;
import com.jihao.aiwiki.domain.search.ScoredPage;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.domain.vault.VaultPathValidator;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.WikiPageService;
import com.jihao.aiwiki.vo.wiki.WikiPageDetailVO;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import com.jihao.aiwiki.vo.wiki.WikiTreeNodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wiki 页面索引服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
@ConditionalOnBean(WikiPageMapper.class)
public class WikiPageServiceImpl implements WikiPageService {

    private final WikiPageMapper wikiPageMapper;
    private final VaultProjectMapper vaultMapper;
    private final VaultFileService fileService;
    private final VaultPathValidator pathValidator;
    private final MarkdownFrontmatterParser frontmatterParser;
    private final KeywordSearchService keywordSearchService;
    private final ObjectMapper objectMapper;

    public WikiPageServiceImpl(WikiPageMapper wikiPageMapper,
                                VaultProjectMapper vaultMapper,
                                VaultFileService fileService,
                                VaultPathValidator pathValidator,
                                MarkdownFrontmatterParser frontmatterParser,
                                KeywordSearchService keywordSearchService,
                                ObjectMapper objectMapper) {
        this.wikiPageMapper = wikiPageMapper;
        this.vaultMapper = vaultMapper;
        this.fileService = fileService;
        this.pathValidator = pathValidator;
        this.frontmatterParser = frontmatterParser;
        this.keywordSearchService = keywordSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<WikiTreeNodeVO> tree(Long vaultId) {
        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());
        Path wikiDir = vaultRoot.resolve("wiki");
        if (!Files.exists(wikiDir)) {
            return List.of();
        }
        return buildTree(wikiDir, vaultRoot);
    }

    @Override
    public WikiPageDetailVO page(Long vaultId, String path) {
        validateWikiPath(path);
        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());

        String content = fileService.readString(vaultRoot, path);
        ParsedFrontmatter fm = frontmatterParser.parse(content);

        return WikiPageDetailVO.builder()
                .path(path)
                .title(fm.getTitle() != null ? fm.getTitle() : filenameWithoutExt(path))
                .type(fm.getType())
                .tags(fm.getTags())
                .related(fm.getRelated())
                .content(content)
                .build();
    }

    @Override
    public List<WikiSearchResultVO> search(Long vaultId, String query) {
        List<WikiPageDO> pages = wikiPageMapper.selectByVaultId(vaultId);
        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());

        List<ScoredPage> candidates = pages.stream().map(p -> {
            String body = loadBody(vaultRoot, p.getPath());
            return new ScoredPage(p.getPath(), p.getTitle(), 0, null, body, p.getType());
        }).toList();

        List<ScoredPage> results = keywordSearchService.search(candidates, query);

        return results.stream().map(r -> WikiSearchResultVO.builder()
                .path(r.getPath())
                .title(r.getTitle())
                .type(r.getType())
                .score(r.getScore())
                .snippet(r.getSnippet())
                .build()).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reindex(Long vaultId, String vaultPath) {
        Path vaultRoot = Path.of(vaultPath);
        Path wikiDir = vaultRoot.resolve("wiki");
        if (!Files.exists(wikiDir)) {
            return;
        }

        List<String> activePaths = new ArrayList<>();
        List<WikiPageDO> toUpsert = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(wikiDir)) {
            stream.filter(p -> !Files.isDirectory(p))
                  .filter(p -> p.toString().endsWith(".md"))
                  .forEach(p -> {
                      String relative = vaultRoot.relativize(p).toString().replace('\\', '/');
                      activePaths.add(relative);
                      try {
                          String content = Files.readString(p);
                          ParsedFrontmatter fm = frontmatterParser.parse(content);
                          String title = fm.getTitle() != null ? fm.getTitle() : filenameWithoutExt(relative);
                          WikiPageDO page = WikiPageDO.builder()
                                  .vaultId(vaultId)
                                  .path(relative)
                                  .title(title)
                                  .type(fm.getType())
                                  .tags(toJson(fm.getTags()))
                                  .related(toJson(fm.getRelated()))
                                  .deleted(0)
                                  .build();
                          toUpsert.add(page);
                      } catch (IOException e) {
                          // skip unreadable files
                      }
                  });
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to walk wiki dir");
        }

        for (WikiPageDO page : toUpsert) {
            wikiPageMapper.upsert(page);
        }
        if (!activePaths.isEmpty()) {
            wikiPageMapper.markDeletedByVaultIdAndPathNotIn(vaultId, activePaths);
        }
    }

    // ---- helpers ----

    private VaultProjectDO requireVault(Long vaultId) {
        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }
        return vault;
    }

    private void validateWikiPath(String path) {
        if (path == null || !path.startsWith("wiki/")) {
            throw new BusinessException(ErrorCode.WIKI_PATH_FORBIDDEN, "path must start with wiki/");
        }
        if (path.contains("..") || path.contains("\\") || path.contains("\0")) {
            throw new BusinessException(ErrorCode.WIKI_PATH_FORBIDDEN, "path contains illegal sequences");
        }
    }

    private String loadBody(Path vaultRoot, String relativePath) {
        try {
            return fileService.readString(vaultRoot, relativePath);
        } catch (BusinessException e) {
            return null;
        }
    }

    private List<WikiTreeNodeVO> buildTree(Path dir, Path vaultRoot) {
        List<WikiTreeNodeVO> nodes = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir).sorted()) {
            entries.forEach(entry -> {
                String relative = vaultRoot.relativize(entry).toString().replace('\\', '/');
                String name = entry.getFileName().toString();
                if (Files.isDirectory(entry)) {
                    WikiTreeNodeVO node = new WikiTreeNodeVO(name, relative, true, buildTree(entry, vaultRoot));
                    nodes.add(node);
                } else if (name.endsWith(".md")) {
                    nodes.add(new WikiTreeNodeVO(name, relative, false, null));
                }
            });
        } catch (IOException e) {
            // return what we have
        }
        return nodes;
    }

    private String filenameWithoutExt(String path) {
        int slash = path.lastIndexOf('/');
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}
