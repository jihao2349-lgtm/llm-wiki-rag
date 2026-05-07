package com.jihao.aiwiki.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    private Long id;
    private Long vaultId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
