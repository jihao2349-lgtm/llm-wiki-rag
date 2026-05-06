package com.jihao.aiwiki.domain.parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析器契约，按文件扩展名选择对应实现。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface DocumentParser {

    /**
     * 判断是否支持该文件名。
     *
     * @param filename 文件名（含扩展名）
     * @return 是否支持
     */
    boolean supports(String filename);

    /**
     * 从输入流提取纯文本内容。
     *
     * @param input    文件输入流
     * @param filename 文件名（含扩展名），部分解析器需要）
     * @return 提取的文本内容
     * @throws IOException 解析 IO 失败
     */
    String extractText(InputStream input, String filename) throws IOException;
}
