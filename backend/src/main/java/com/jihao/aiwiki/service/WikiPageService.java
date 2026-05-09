package com.jihao.aiwiki.service;

import com.jihao.aiwiki.vo.wiki.WikiPageDetailVO;
import com.jihao.aiwiki.vo.wiki.WikiSearchResultVO;
import com.jihao.aiwiki.vo.wiki.WikiTreeNodeVO;

import java.util.List;

/**
 * Wiki 页面索引服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface WikiPageService {

    /**
     * 获取 wiki/ 目录文件树。
     *
     * @param vaultId Vault ID
     * @return 文件树根节点列表
     */
    List<WikiTreeNodeVO> tree(Long vaultId);

    /**
     * 获取 Wiki 页面详情（含 Markdown 全文）。
     *
     * @param vaultId Vault ID
     * @param path    Wiki 相对路径（必须以 wiki/ 开头）
     * @return 页面详情
     */
    WikiPageDetailVO page(Long vaultId, String path);

    /**
     * 关键词搜索 Wiki 页面。
     *
     * @param vaultId Vault ID
     * @param query   关键词
     * @return 搜索结果列表，按相关度排序
     */
    List<WikiSearchResultVO> search(Long vaultId, String query);

    /**
     * 重建 Vault 的 wiki_page 索引（扫描 wiki/ 目录）。
     *
     * @param vaultId   Vault ID
     * @param vaultPath Vault 根目录绝对路径
     * @return 当前索引的 Wiki 页面数量
     */
    int reindex(Long vaultId, String vaultPath);
}
