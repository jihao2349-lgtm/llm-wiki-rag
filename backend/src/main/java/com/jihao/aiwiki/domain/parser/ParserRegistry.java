package com.jihao.aiwiki.domain.parser;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 文档解析器注册表，按文件名选择合适的解析器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class ParserRegistry {

    private final List<DocumentParser> parsers;

    public ParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * 按文件名查找第一个支持的解析器。
     *
     * @param filename 文件名或 URL
     * @return 解析器，无匹配时返回空
     */
    public Optional<DocumentParser> find(String filename) {
        return parsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst();
    }
}
