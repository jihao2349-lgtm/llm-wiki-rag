package com.jihao.aiwiki.service;

import com.jihao.aiwiki.common.PageResult;
import com.jihao.aiwiki.vo.source.SourceDocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 资料导入与解析服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface SourceDocumentService {

    /**
     * 上传文件，保存到 raw/sources/files/，解析提取文本。
     *
     * @param vaultId Vault ID
     * @param file    上传文件
     * @return 资料详情
     */
    SourceDocumentVO upload(Long vaultId, MultipartFile file);

    /**
     * 导入网页 URL，抓取并解析。
     *
     * @param vaultId Vault ID
     * @param url     目标 URL
     * @return 资料详情
     */
    SourceDocumentVO importUrl(Long vaultId, String url);

    /**
     * 分页查询资料列表。
     *
     * @param vaultId Vault ID
     * @param type    类型过滤，null 不过滤
     * @param status  状态过滤，null 不过滤
     * @param pageNo  页码（从 1 开始）
     * @param pageSize 每页数量
     * @return 分页结果
     */
    PageResult<SourceDocumentVO> page(Long vaultId, String type, String status, int pageNo, int pageSize);

    /**
     * 获取资料详情。
     *
     * @param id 资料 ID
     * @return 资料详情
     */
    SourceDocumentVO detail(Long id);

    /**
     * 获取解析文本预览（最多 2000 字符）。
     *
     * @param id 资料 ID
     * @return 预览文本
     */
    String preview(Long id);

    /**
     * 手动重新解析资料。
     *
     * @param id 资料 ID
     * @return 更新后详情
     */
    SourceDocumentVO reparse(Long id);
}
