package com.jihao.aiwiki.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建对话会话请求 DTO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class CreateSessionDTO {

    @NotNull
    private Long vaultId;

    @NotBlank
    private String title;
}
