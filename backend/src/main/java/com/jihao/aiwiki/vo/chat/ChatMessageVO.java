package com.jihao.aiwiki.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话消息 VO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private List<String> citations;
    private String savedPath;
    private LocalDateTime createTime;
}
