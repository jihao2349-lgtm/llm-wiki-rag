package com.jihao.aiwiki.mapper;

import com.jihao.aiwiki.entity.ChatMessageDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 对话消息 MyBatis Mapper。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Mapper
public interface ChatMessageMapper {

    void insert(ChatMessageDO message);

    ChatMessageDO selectById(@Param("id") Long id);

    List<ChatMessageDO> selectBySessionId(@Param("sessionId") Long sessionId);

    void updateSavedPath(@Param("id") Long id, @Param("savedPath") String savedPath);
}
