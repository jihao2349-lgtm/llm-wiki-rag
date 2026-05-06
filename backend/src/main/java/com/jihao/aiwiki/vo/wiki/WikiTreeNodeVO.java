package com.jihao.aiwiki.vo.wiki;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wiki 文件树节点 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WikiTreeNodeVO {

    /** 文件/目录名称 */
    private String name;

    /** 相对于 Vault 的路径 */
    private String path;

    /** 是否为目录 */
    private boolean directory;

    /** 子节点（目录时有值） */
    private List<WikiTreeNodeVO> children;
}
