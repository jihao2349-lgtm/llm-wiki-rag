package com.jihao.aiwiki.domain.ingest.pipeline;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * LLM 输出中解析出的单个 FILE block。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@AllArgsConstructor
public class FileBlock {

    /** Wiki 相对路径，必须以 wiki/ 开头 */
    private String path;

    /** Markdown 全文（含 frontmatter） */
    private String content;
}
