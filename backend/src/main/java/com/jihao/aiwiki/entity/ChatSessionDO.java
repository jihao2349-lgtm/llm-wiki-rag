package com.jihao.aiwiki.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话数据库实体，映射 chat_session 表。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDO {

    private Long id;
    private Long vaultId;
    private String title;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
