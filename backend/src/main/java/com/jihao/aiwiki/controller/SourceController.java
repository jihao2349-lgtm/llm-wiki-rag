package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.common.BusinessException;
import com.jihao.aiwiki.common.ErrorCode;
import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.dto.source.SourceUrlImportDTO;
import com.jihao.aiwiki.dto.task.IngestTaskCreateRequest;
import com.jihao.aiwiki.service.IngestTaskService;
import com.jihao.aiwiki.service.SourceDocumentService;
import com.jihao.aiwiki.vo.source.SourceDocumentVO;
import com.jihao.aiwiki.vo.task.IngestTaskVO;
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
    private final IngestTaskService ingestTaskService;

    public SourceController(ObjectProvider<SourceDocumentService> sourceServiceProvider,
                            IngestTaskService ingestTaskService) {
        this.sourceServiceProvider = sourceServiceProvider;
        this.ingestTaskService = ingestTaskService;
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
     * 导入网页 URL —— 暂未开放，返回 501。
     */
    @PostMapping("/import-url")
    public ApiResponse<SourceDocumentVO> importUrl(@Valid @RequestBody SourceUrlImportDTO dto) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "URL 抓取功能暂未开放");
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

    /**
     * 手动触发 AI 摄入（创建摄入任务）。
     */
    @PostMapping("/{id}/ingest")
    public ApiResponse<IngestTaskVO> ingest(@PathVariable Long id) {
        SourceDocumentVO source = svc().detail(id);
        IngestTaskCreateRequest req = new IngestTaskCreateRequest();
        req.setVaultId(source.getVaultId());
        req.setSourceId(id);
        return ApiResponse.success(ingestTaskService.createTask(req));
    }
}
