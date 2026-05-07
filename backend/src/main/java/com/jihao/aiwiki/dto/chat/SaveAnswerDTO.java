package com.jihao.aiwiki.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存回答到 Wiki 请求 DTO。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Data
public class SaveAnswerDTO {

    @NotNull
    private Long messageId;

    /**
     * 目标路径，必须以 wiki/synthesis/ 或 wiki/questions/ 开头。
     */
    @NotBlank
    private String targetPath;
}
