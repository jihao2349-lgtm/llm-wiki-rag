package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.domain.search.ScoredPage;
import com.jihao.aiwiki.entity.WikiPageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Wiki 页面索引 MyBatis Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface WikiPageMapper {

    /**
     * 插入页面记录，回写生成主键。
     *
     * @param page 页面实体
     */
    void insert(WikiPageDO page);

    /**
     * 按主键查询（逻辑未删除）。
     *
     * @param id 主键
     * @return 页面实体，不存在时返回 null
     */
    WikiPageDO selectById(Long id);

    /**
     * 按 Vault 和路径查询（逻辑未删除）。
     *
     * @param vaultId Vault ID
     * @param path    Wiki 相对路径
     * @return 页面实体，不存在时返回 null
     */
    WikiPageDO selectByVaultIdAndPath(@Param("vaultId") Long vaultId, @Param("path") String path);

    /**
     * 查询 Vault 所有未删除页面。
     *
     * @param vaultId Vault ID
     * @return 页面列表
     */
    List<WikiPageDO> selectByVaultId(@Param("vaultId") Long vaultId);

    /**
     * 按 Vault 统计页面数量（逻辑未删除）。
     *
     * @param vaultId Vault ID
     * @return 页面数量
     */
    long countByVaultId(@Param("vaultId") Long vaultId);

    /**
     * INSERT ON DUPLICATE KEY UPDATE，按 vault_id + path 唯一键幂等写入。
     *
     * @param page 页面实体
     */
    void upsert(WikiPageDO page);

    /**
     * 将不在 activePaths 列表中的页面标记为逻辑删除。
     *
     * @param vaultId     Vault ID
     * @param activePaths 当前存活页面路径列表
     */
    void markDeletedByVaultIdAndPathNotIn(@Param("vaultId") Long vaultId,
                                          @Param("activePaths") List<String> activePaths);

    /**
     * 按 embed_status 统计页面数量。
     *
     * @param vaultId     Vault ID
     * @param embedStatus 状态值
     * @return 数量
     */
    long countByEmbedStatus(@Param("vaultId") Long vaultId, @Param("embedStatus") String embedStatus);

    /**
     * 按 embed_status 批量查询页面 ID 列表（用于分批向量化）。
     *
     * @param vaultId      Vault ID
     * @param embedStatuses 状态列表（PENDING / FAILED）
     * @return 页面 ID 列表
     */
    List<Long> selectIdsByEmbedStatuses(@Param("vaultId") Long vaultId,
                                        @Param("embedStatuses") List<String> embedStatuses);

    /**
     * 查询最近一次成功向量化的时间。
     *
     * @param vaultId Vault ID
     * @return 最近向量化时间，无记录时为 null
     */
    LocalDateTime selectLastEmbeddedAt(@Param("vaultId") Long vaultId);

    /**
     * 更新页面向量化成功状态。
     *
     * @param id               页面 ID
     * @param embeddingJson    向量 JSON 字符串 ("[0.1, 0.2, ...]")
     * @param model            向量模型名
     * @param embedContentHash 内容哈希
     * @param embeddedAt       向量化时间
     */
    void updateEmbeddingSuccess(@Param("id") Long id,
                                @Param("embeddingJson") String embeddingJson,
                                @Param("model") String model,
                                @Param("embedContentHash") String embedContentHash,
                                @Param("embeddedAt") LocalDateTime embeddedAt);

    /**
     * 更新页面向量化失败状态。
     *
     * @param id    页面 ID
     * @param error 失败原因
     */
    void updateEmbeddingFailed(@Param("id") Long id, @Param("error") String error);

    /**
     * 将页面 embed_status 重置为 PENDING（用于重试）。
     *
     * @param id 页面 ID
     */
    void resetEmbedStatusToPending(@Param("id") Long id);

    /**
     * 向量近似检索：返回与 queryVec 最相似的 topK 页面。
     *
     * @param vaultId      Vault ID
     * @param queryVecJson 查询向量 JSON 字符串
     * @param topK         返回数量
     * @return 按 distance 升序排列的结果列表
     */
    List<ScoredPage> vectorSearch(@Param("vaultId") Long vaultId,
                                  @Param("queryVecJson") String queryVecJson,
                                  @Param("topK") int topK);

    /**
     * 查询 Vault 下有 embed_status != SUCCESS 的页面数量（用于 Dashboard 提醒）。
     *
     * @param vaultId Vault ID
     * @return 未向量化页面数量
     */
    long countNonSuccessEmbeds(@Param("vaultId") Long vaultId);

    /**
     * 查询 embed_status = FAILED 的页面列表（含路径和错误信息）。
     *
     * @param vaultId Vault ID
     * @return 失败页面列表
     */
    List<WikiPageDO> selectFailedEmbeds(@Param("vaultId") Long vaultId);
}
