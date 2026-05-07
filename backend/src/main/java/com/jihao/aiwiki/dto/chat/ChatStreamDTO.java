package com.jihao.aiwiki.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 流式问答请求 DTO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class ChatStreamDTO {

    @NotNull
    private Long vaultId;

    @NotNull
    private Long sessionId;

    @NotBlank
    private String question;

    /** 最大引用数量，默认 5 */
    private int maxReferences = 5;

    /** LLM 上下文最大 token 数，≤0 使用默认值 */
    private int maxContextTokens = 0;
}
