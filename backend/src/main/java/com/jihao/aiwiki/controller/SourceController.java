package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.dto.source.SourceUrlImportDTO;
import com.jihao.aiwiki.service.SourceDocumentService;
import com.jihao.aiwiki.vo.source.SourceDocumentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资料导入与解析 API 控制器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final ObjectProvider<SourceDocumentService> sourceServiceProvider;

    public SourceController(ObjectProvider<SourceDocumentService> sourceServiceProvider) {
        this.sourceServiceProvider = sourceServiceProvider;
    }

    private SourceDocumentService svc() {
        return sourceServiceProvider.getIfAvailable(() -> {
            throw new IllegalStateException("SourceDocumentService not available");
        });
    }

    /**
     * 上传文件并保存到 raw/sources/files/。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SourceDocumentVO> upload(
            @RequestParam Long vaultId,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(svc().upload(vaultId, file));
    }

    /**
     * 导入网页 URL 并抓取解析。
     */
    @PostMapping("/import-url")
    public ApiResponse<SourceDocumentVO> importUrl(@Valid @RequestBody SourceUrlImportDTO dto) {
        return ApiResponse.success(svc().importUrl(dto.getVaultId(), dto.getUrl()));
    }

    /**
     * 分页查询资料列表。
     */
    @GetMapping("/page")
    public ApiResponse<PageResult<SourceDocumentVO>> page(
            @RequestParam Long vaultId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(svc().page(vaultId, type, status, pageNo, pageSize));
    }

    /**
     * 获取资料详情。
     */
    @GetMapping("/detail")
    public ApiResponse<SourceDocumentVO> detail(@RequestParam Long id) {
        return ApiResponse.success(svc().detail(id));
    }

    /**
     * 获取解析文本预览（最多 2000 字符）。
     */
    @GetMapping("/preview")
    public ApiResponse<String> preview(@RequestParam Long id) {
        return ApiResponse.success(svc().preview(id));
    }

    /**
     * 手动重新解析资料。
     */
    @PostMapping("/{id}/parse")
    public ApiResponse<SourceDocumentVO> reparse(@PathVariable Long id) {
        return ApiResponse.success(svc().reparse(id));
    }
}
