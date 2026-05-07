package com.jihao.aiwiki.service;

import com.jihao.aiwiki.dto.chat.ChatStreamDTO;
import com.jihao.aiwiki.dto.chat.CreateSessionDTO;
import com.jihao.aiwiki.dto.chat.SaveAnswerDTO;
import com.jihao.aiwiki.vo.chat.ChatMessageVO;
import com.jihao.aiwiki.vo.chat.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Vault 对话服务契约。
 *
 * @author jihao
 * @date 2026/05/06
 */
public interface ChatService {

    /**
     * 创建对话会话。
     */
    ChatSessionVO createSession(CreateSessionDTO dto);

    /**
     * 查询 Vault 下所有会话。
     */
    List<ChatSessionVO> listSessions(Long vaultId);

    /**
     * 查询会话下所有消息。
     */
    List<ChatMessageVO> listMessages(Long sessionId);

    /**
     * 流式问答，通过 SSE 推送 reference / delta / done / error 事件。
     */
    void streamChat(ChatStreamDTO dto, SseEmitter emitter);

    /**
     * 保存 AI 回答到 Vault Wiki。
     */
    ChatMessageVO saveAnswer(Long vaultId, SaveAnswerDTO dto);
}
