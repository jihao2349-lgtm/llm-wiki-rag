package com.jihao.aiwiki.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一 API 响应体契约测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class ApiResponseTest {

    /**
     * 验证成功响应保持共享响应结构。
     */
    @Test
    void successShouldUseSharedShape() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo("ok");
    }

    /**
     * 验证失败响应保持共享响应结构。
     */
    @Test
    void failShouldUseSharedShape() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.NOT_FOUND);

        assertThat(response.getCode()).isEqualTo(404);
        assertThat(response.getMessage()).isEqualTo("not found");
        assertThat(response.getData()).isNull();
    }
}
