package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.service.WikiPageService;
import com.jihao.aiwiki.vo.wiki.WikiPageDetailVO;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import com.jihao.aiwiki.vo.wiki.WikiTreeNodeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Wiki 页面 API 控制器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiController {

    private final WikiPageService wikiPageService;

    public WikiController(WikiPageService wikiPageService) {
        this.wikiPageService = wikiPageService;
    }

    /**
     * 获取 wiki/ 目录文件树。
     */
    @GetMapping("/tree")
    public ApiResponse<List<WikiTreeNodeVO>> tree(@RequestParam Long vaultId) {
        return ApiResponse.success(wikiPageService.tree(vaultId));
    }

    /**
     * 获取 Wiki 页面详情（含 Markdown 全文）。
     */
    @GetMapping("/page")
    public ApiResponse<WikiPageDetailVO> page(
            @RequestParam Long vaultId,
            @RequestParam String path) {
        return ApiResponse.success(wikiPageService.page(vaultId, path));
    }

    /**
     * 关键词搜索 Wiki 页面。
     */
    @GetMapping("/search")
    public ApiResponse<List<WikiSearchResultVO>> search(
            @RequestParam Long vaultId,
            @RequestParam String query) {
        return ApiResponse.success(wikiPageService.search(vaultId, query));
    }
}
