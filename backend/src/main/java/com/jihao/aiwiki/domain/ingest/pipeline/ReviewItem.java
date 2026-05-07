package com.jihao.aiwiki.domain.ingest.pipeline;

import java.util.List;

/**
 * LLM 输出中解析出的单个 REVIEW block，表示需要人工判断的项目。
 * type: contradiction / duplicate / missing-page / suggestion
 */
public record ReviewItem(
        String type,
        String title,
        String description,
        List<String> options,
        List<String> pages,
        List<String> searches
) {}
