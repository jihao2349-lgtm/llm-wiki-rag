package com.jihao.aiwiki.domain.ingest.pipeline;

import java.util.List;

/**
 * FileBlockParser 解析结果，包含成功解析的 blocks 和解析警告。
 */
public record ParseResult(List<FileBlock> blocks, List<String> warnings) {}
