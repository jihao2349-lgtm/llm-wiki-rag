package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.domain.embedding.EmbeddingClient;
import com.jihao.aiwiki.domain.embedding.EmbeddingConfig;
import com.jihao.aiwiki.domain.embedding.EmbeddingException;
import com.jihao.aiwiki.domain.embedding.EmbeddingProgress;
import com.jihao.aiwiki.domain.embedding.EmbeddingResult;
import com.jihao.aiwiki.domain.embedding.EmbeddingService;
import com.jihao.aiwiki.domain.embedding.EmbeddingStats;
import com.jihao.aiwiki.dto.embedding.EmbeddingRebuildRequest;
import com.jihao.aiwiki.dto.embedding.EmbeddingTestRequest;
import com.jihao.aiwiki.entity.WikiPageDO;
import com.jihao.aiwiki.mapper.WikiPageMapper;
import com.jihao.aiwiki.service.SettingService;
import com.jihao.aiwiki.vo.embedding.EmbeddingFailedPageVO;
import com.jihao.aiwiki.vo.embedding.EmbeddingProgressVO;
import com.jihao.aiwiki.vo.embedding.EmbeddingStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Embedding 管理 API。
 *
 * @author jihao
 * @date 2026/05/08
 */
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;
    private final EmbeddingClient embeddingClient;
    private final WikiPageMapper wikiPageMapper;
    private final SettingService settingService;

    /**
     * 获取 vault 向量化统计（含失败页面列表）。
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@RequestParam Long vaultId) {
        EmbeddingStats stats = embeddingService.stats(vaultId);
        List<WikiPageDO> failedPages = wikiPageMapper.selectFailedEmbeds(vaultId);
        List<EmbeddingFailedPageVO> failedVOs = failedPages.stream()
                .map(p -> EmbeddingFailedPageVO.builder()
                        .pageId(p.getId())
                        .path(p.getPath())
                        .title(p.getTitle())
                        .error(p.getEmbedError())
                        .build())
                .toList();

        return ApiResponse.success(Map.of(
                "total", stats.getTotal(),
                "success", stats.getSuccess(),
                "failed", stats.getFailed(),
                "pending", stats.getPending(),
                "lastEmbeddedAt", stats.getLastEmbeddedAt() != null ? stats.getLastEmbeddedAt().toString() : "",
                "failedPages", failedVOs
        ));
    }

    /**
     * 获取 vault 下所有 Wiki 页面的向量化状态，用于单页向量化操作。
     */
    @GetMapping("/pages")
    public ApiResponse<List<Map<String, Object>>> pages(@RequestParam Long vaultId) {
        List<WikiPageDO> pages = wikiPageMapper.selectByVaultId(vaultId);
        return ApiResponse.success(pages.stream()
                .map(page -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pageId", page.getId());
                    item.put("path", page.getPath());
                    item.put("title", page.getTitle());
                    item.put("type", page.getType());
                    item.put("embedStatus", page.getEmbedStatus());
                    item.put("embeddingModel", page.getEmbeddingModel() == null ? "" : page.getEmbeddingModel());
                    item.put("embeddedAt", page.getEmbeddedAt() == null ? "" : page.getEmbeddedAt().toString());
                    item.put("error", page.getEmbedError() == null ? "" : page.getEmbedError());
                    return item;
                })
                .toList());
    }

    /**
     * 测试 Embedding 配置连通性，返回实际向量维度。
     */
    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(@RequestBody @Valid EmbeddingTestRequest request) {
        EmbeddingConfig savedConfig = settingService.getEmbeddingConfig();
        EmbeddingConfig config = EmbeddingConfig.builder()
                .enabled(true)
                .baseUrl(request.getBaseUrl())
                .apiKey(StringUtils.hasText(request.getApiKey()) ? request.getApiKey() : savedConfig.getApiKey())
                .model(request.getModel())
                .dimension(request.getDimension() != null ? request.getDimension() : 1024)
                .batchSize(1)
                .build();
        try {
            float[] vec = embeddingClient.embedSingle("connectivity test", config);
            return ApiResponse.success(Map.of(
                    "success", true,
                    "dimension", vec.length,
                    "message", "Embedding API connectivity OK"
            ));
        } catch (EmbeddingException e) {
            return ApiResponse.success(Map.of(
                    "success", false,
                    "dimension", 0,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 异步触发批量向量化。
     * mode: pending（默认）| failed | all
     */
    @PostMapping("/rebuild")
    public ApiResponse<Map<String, Object>> rebuild(@RequestBody @Valid EmbeddingRebuildRequest request) {
        if (embeddingService.isRunning(request.getVaultId())) {
            return ApiResponse.success(Map.of(
                    "started", false,
                    "message", "向量化任务正在运行中，请稍后再试"
            ));
        }

        List<Long> pageIds = resolvePageIds(request);
        if (pageIds.isEmpty()) {
            return ApiResponse.success(Map.of(
                    "started", false,
                    "message", "没有需要处理的页面"
            ));
        }

        embeddingService.embedPagesBatch(pageIds, request.getVaultId());
        return ApiResponse.success(Map.of(
                "started", true,
                "total", pageIds.size(),
                "message", "批量向量化已启动，共 " + pageIds.size() + " 个页面"
        ));
    }

    /**
     * 查询批量向量化进度（从 Redis 读取）。
     */
    @GetMapping("/progress")
    public ApiResponse<EmbeddingProgressVO> progress(@RequestParam Long vaultId) {
        EmbeddingProgress p = embeddingService.getProgress(vaultId);
        return ApiResponse.success(EmbeddingProgressVO.builder()
                .processing(p.isProcessing())
                .current(p.getCurrent())
                .total(p.getTotal())
                .build());
    }

    /**
     * 手动同步向量化单个页面。
     */
    @PostMapping("/page/{pageId}")
    public ApiResponse<Map<String, Object>> embedPage(@PathVariable Long pageId) {
        EmbeddingResult result = embeddingService.embedPage(pageId);
        if (result.success()) {
            String msg = result.skipped() ? "内容未变化，跳过向量化" : "向量化成功";
            return ApiResponse.success(Map.of("success", true, "skipped", result.skipped(), "message", msg));
        }
        return ApiResponse.success(Map.of("success", false, "skipped", false, "message", result.error()));
    }

    // ---- helpers ----

    private List<Long> resolvePageIds(EmbeddingRebuildRequest request) {
        return switch (request.getMode()) {
            case "failed" -> wikiPageMapper.selectIdsByEmbedStatuses(
                    request.getVaultId(), List.of("FAILED"));
            case "all" -> wikiPageMapper.selectIdsByEmbedStatuses(
                    request.getVaultId(), List.of("PENDING", "FAILED", "SUCCESS"));
            default -> wikiPageMapper.selectIdsByEmbedStatuses(
                    request.getVaultId(), List.of("PENDING", "FAILED"));
        };
    }
}
