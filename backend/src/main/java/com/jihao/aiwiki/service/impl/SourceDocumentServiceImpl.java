package com.jihao.aiwiki.service.impl;

import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.domain.parser.DocumentParser;
import com.jihao.aiwiki.domain.parser.ParserRegistry;
import com.jihao.aiwiki.domain.parser.UrlFetchParser;
import com.jihao.aiwiki.domain.vault.VaultFileService;
import com.jihao.aiwiki.entity.SourceDocumentDO;
import com.jihao.aiwiki.entity.VaultProjectDO;
import com.jihao.aiwiki.mapper.SourceDocumentMapper;
import com.jihao.aiwiki.mapper.VaultProjectMapper;
import com.jihao.aiwiki.service.SourceDocumentService;
import com.jihao.aiwiki.vo.source.SourceDocumentVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * 资料导入与解析服务实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Service
@ConditionalOnBean(SourceDocumentMapper.class)
public class SourceDocumentServiceImpl implements SourceDocumentService {

    private static final String STATUS_PARSED = "PARSED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String TYPE_FILE = "FILE";
    private static final String TYPE_URL = "URL";

    private final SourceDocumentMapper sourceMapper;
    private final VaultProjectMapper vaultMapper;
    private final VaultFileService fileService;
    private final ParserRegistry parserRegistry;
    private final UrlFetchParser urlFetchParser;

    public SourceDocumentServiceImpl(SourceDocumentMapper sourceMapper,
                                     VaultProjectMapper vaultMapper,
                                     VaultFileService fileService,
                                     ParserRegistry parserRegistry,
                                     UrlFetchParser urlFetchParser) {
        this.sourceMapper = sourceMapper;
        this.vaultMapper = vaultMapper;
        this.fileService = fileService;
        this.parserRegistry = parserRegistry;
        this.urlFetchParser = urlFetchParser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourceDocumentVO upload(Long vaultId, MultipartFile file) {
        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());

        String originalFilename = file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "unknown";
        String slug = slugify(stripExtension(originalFilename));
        String ext = getExtension(originalFilename);
        String relativePath = resolveUniquePath("raw/sources/files/", slug, ext, vaultRoot);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to read uploaded file");
        }

        String hash = sha256Hex(bytes);

        fileService.writeBytesAtomically(vaultRoot, relativePath, bytes);

        String extractedText = parseBytes(bytes, originalFilename);
        String cachePath = "cache/" + slug + "-" + hash.substring(0, 8) + ".txt";
        if (extractedText != null) {
            fileService.writeBytesAtomically(vaultRoot, ".ai-wiki/" + cachePath,
                    extractedText.getBytes(StandardCharsets.UTF_8));
        }

        SourceDocumentDO doc = SourceDocumentDO.builder()
                .vaultId(vaultId)
                .type(TYPE_FILE)
                .title(stripExtension(originalFilename))
                .originalPath(relativePath)
                .originalHash(hash)
                .extractedTextPath(extractedText != null ? ".ai-wiki/" + cachePath : null)
                .status(extractedText != null ? STATUS_PARSED : STATUS_FAILED)
                .errorMessage(extractedText != null ? null : "no supported parser for " + originalFilename)
                .deleted(0)
                .build();
        sourceMapper.insert(doc);

        return toVO(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourceDocumentVO importUrl(Long vaultId, String url) {
        VaultProjectDO vault = requireVault(vaultId);
        Path vaultRoot = Path.of(vault.getPath());

        String text;
        try {
            text = urlFetchParser.fetch(url);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to fetch url: " + e.getMessage());
        }

        String slug = slugify(url.replaceAll("https?://", "").replaceAll("[^a-zA-Z0-9]", "-"));
        if (slug.length() > 60) slug = slug.substring(0, 60);
        String hash = sha256Hex(url.getBytes(StandardCharsets.UTF_8));
        String webclipPath = "raw/sources/webclips/" + slug + ".txt";
        String cachePath = ".ai-wiki/cache/" + slug + "-" + hash.substring(0, 8) + ".txt";

        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        fileService.writeBytesAtomically(vaultRoot, webclipPath, textBytes);
        fileService.writeBytesAtomically(vaultRoot, cachePath, textBytes);

        String title = extractFirstLine(text);

        SourceDocumentDO doc = SourceDocumentDO.builder()
                .vaultId(vaultId)
                .type(TYPE_URL)
                .title(title)
                .originalPath(webclipPath)
                .originalHash(hash)
                .extractedTextPath(cachePath)
                .sourceUrl(url)
                .status(STATUS_PARSED)
                .deleted(0)
                .build();
        sourceMapper.insert(doc);

        return toVO(doc);
    }

    @Override
    public PageResult<SourceDocumentVO> page(Long vaultId, String type, String status,
                                              int pageNo, int pageSize) {
        int offset = (pageNo - 1) * pageSize;
        List<SourceDocumentDO> docs = sourceMapper.selectPage(vaultId, type, status, offset, pageSize);
        long total = sourceMapper.countPage(vaultId, type, status);
        List<SourceDocumentVO> vos = docs.stream().map(this::toVO).toList();
        return new PageResult<>(vos, total, (long) pageNo, (long) pageSize);
    }

    @Override
    public SourceDocumentVO detail(Long id) {
        SourceDocumentDO doc = sourceMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND, "source not found: " + id);
        }
        return toVO(doc);
    }

    @Override
    public String preview(Long id) {
        SourceDocumentDO doc = sourceMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND, "source not found: " + id);
        }
        if (doc.getExtractedTextPath() == null) {
            return "";
        }
        VaultProjectDO vault = requireVault(doc.getVaultId());
        try {
            byte[] bytes = fileService.readBytes(Path.of(vault.getPath()), doc.getExtractedTextPath());
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.length() > 2000 ? text.substring(0, 2000) + "…" : text;
        } catch (BusinessException e) {
            return "";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SourceDocumentVO reparse(Long id) {
        SourceDocumentDO doc = sourceMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ErrorCode.SOURCE_NOT_FOUND, "source not found: " + id);
        }
        VaultProjectDO vault = requireVault(doc.getVaultId());
        Path vaultRoot = Path.of(vault.getPath());

        byte[] bytes;
        try {
            bytes = fileService.readBytes(vaultRoot, doc.getOriginalPath());
        } catch (BusinessException e) {
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMessage("original file not found");
            sourceMapper.updateByPrimaryKey(doc);
            return toVO(doc);
        }

        String filename = doc.getOriginalPath();
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) filename = filename.substring(slash + 1);

        String text = parseBytes(bytes, filename);
        if (text != null && doc.getExtractedTextPath() != null) {
            fileService.writeBytesAtomically(vaultRoot, doc.getExtractedTextPath(),
                    text.getBytes(StandardCharsets.UTF_8));
            doc.setStatus(STATUS_PARSED);
            doc.setErrorMessage(null);
        } else {
            doc.setStatus(STATUS_FAILED);
            doc.setErrorMessage("no supported parser");
        }
        sourceMapper.updateByPrimaryKey(doc);
        return toVO(doc);
    }

    // ---- helpers ----

    private VaultProjectDO requireVault(Long vaultId) {
        VaultProjectDO vault = vaultMapper.selectById(vaultId);
        if (vault == null) {
            throw new BusinessException(ErrorCode.VAULT_NOT_INITIALIZED, "vault not found: " + vaultId);
        }
        return vault;
    }

    private String parseBytes(byte[] bytes, String filename) {
        return parserRegistry.find(filename).map(parser -> {
            try {
                return parser.extractText(new ByteArrayInputStream(bytes), filename);
            } catch (IOException e) {
                return null;
            }
        }).orElse(null);
    }

    private String resolveUniquePath(String dir, String slug, String ext, Path vaultRoot) {
        String candidate = dir + slug + ext;
        try {
            // check if file already exists
            fileService.readBytes(vaultRoot, candidate);
            // file exists, try numbered variants
            for (int i = 2; i <= 100; i++) {
                String numbered = dir + slug + "-" + i + ext;
                try {
                    fileService.readBytes(vaultRoot, numbered);
                } catch (BusinessException e) {
                    return numbered;
                }
            }
        } catch (BusinessException e) {
            // file doesn't exist, use original
        }
        return candidate;
    }

    private String slugify(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "0";
        }
    }

    private String extractFirstLine(String text) {
        if (text == null || text.isBlank()) return "Untitled";
        String first = text.lines().findFirst().orElse("Untitled").trim();
        return first.length() > 200 ? first.substring(0, 200) : first;
    }

    private SourceDocumentVO toVO(SourceDocumentDO doc) {
        return SourceDocumentVO.builder()
                .id(doc.getId())
                .vaultId(doc.getVaultId())
                .type(doc.getType())
                .title(doc.getTitle())
                .originalPath(doc.getOriginalPath())
                .sourceUrl(doc.getSourceUrl())
                .status(doc.getStatus())
                .errorMessage(doc.getErrorMessage())
                .createTime(doc.getCreateTime())
                .build();
    }
}
