package com.jihao.aiwiki.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应体。
 * 所有接口返回值均包装为此结构，与前端约定 code=200 为成功。
 *
 * @param <T> 业务数据类型
 * @author jihao
 * @date 2026/05/06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class ApiResponse<T> {

    /** 响应码，200 表示成功 */
    @Schema(description = "响应码，200 表示成功", example = "200")
    private Integer code;

    /** 响应消息 */
    @Schema(description = "响应消息", example = "success")
    private String message;

    /** 业务数据 */
    @Schema(description = "业务数据")
    private T data;

    /**
     * 成功响应（无数据）。
     *
     * @return 成功响应体
     */
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功响应（带数据）。
     *
     * @param <T> response payload type
     * @return 成功响应体
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 失败响应（使用预定义错误码）。
     *
     * @param errorCode 预定义错误码
     * @return 失败响应体
     */
    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应（自定义错误消息）。
     *
     * @param message human-readable response message
     * @return 失败响应体
     */
    public static ApiResponse<Void> fail(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
