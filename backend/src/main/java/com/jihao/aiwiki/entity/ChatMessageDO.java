package com.jihao.aiwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话消息数据库实体，映射 chat_message 表。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDO {

    private Long id;
    private Long sessionId;
    /** user / assistant / system */
    private String role;
    private String content;
    /** JSON array of citation paths */
    private String citations;
    /** Path in vault if answer was saved */
    private String savedPath;
    private LocalDateTime createTime;
}
