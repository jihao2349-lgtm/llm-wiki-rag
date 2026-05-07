package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.ChatSessionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话会话 MyBatis Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface ChatSessionMapper {

    void insert(ChatSessionDO session);

    ChatSessionDO selectById(@Param("id") Long id);

    List<ChatSessionDO> selectByVaultId(@Param("vaultId") Long vaultId);
}
