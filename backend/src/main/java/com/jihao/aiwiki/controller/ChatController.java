package com.jihao.aiwiki.controller;

import com.jihao.aiwiki.common.ApiResponse;
import com.jihao.aiwiki.dto.chat.ChatStreamDTO;
import com.jihao.aiwiki.dto.chat.CreateSessionDTO;
import com.jihao.aiwiki.dto.chat.SaveAnswerDTO;
import com.jihao.aiwiki.service.ChatService;
import com.jihao.aiwiki.vo.chat.ChatMessageVO;
import com.jihao.aiwiki.vo.chat.ChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 对话 API 控制器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 创建对话会话。
     */
    @PostMapping("/session")
    public ApiResponse<ChatSessionVO> createSession(@Valid @RequestBody CreateSessionDTO dto) {
        return ApiResponse.success(chatService.createSession(dto));
    }

    /**
     * 查询 Vault 下所有会话。
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionVO>> listSessions(@RequestParam Long vaultId) {
        return ApiResponse.success(chatService.listSessions(vaultId));
    }

    /**
     * 查询会话下所有消息。
     */
    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageVO>> listMessages(@RequestParam Long sessionId) {
        return ApiResponse.success(chatService.listMessages(sessionId));
    }

    /**
     * SSE 流式问答。推送 reference、delta、done、error 事件。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatStreamDTO dto) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout
        chatService.streamChat(dto, emitter);
        return emitter;
    }

    /**
     * 保存 AI 回答到 Vault Wiki（wiki/synthesis/ 或 wiki/questions/）。
     */
    @PostMapping("/save-answer")
    public ApiResponse<ChatMessageVO> saveAnswer(
            @RequestParam Long vaultId,
            @Valid @RequestBody SaveAnswerDTO dto) {
        return ApiResponse.success(chatService.saveAnswer(vaultId, dto));
    }
}
