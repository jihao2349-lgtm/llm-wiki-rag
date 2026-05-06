package com.jihao.aiwiki.common;

/**
 * 业务异常。
 * 携带稳定错误码，用于统一异常处理。
 *
 * @author jihao
 * @date 2026/05/06
 */
public class BusinessException extends RuntimeException {

    /** 稳定 API 错误码 */
    private final ErrorCode errorCode;

    /**
     * 使用默认错误消息创建业务异常。
     *
     * @param errorCode 稳定 API 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义错误消息创建业务异常。
     *
     * @param errorCode 稳定 API 错误码
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 获取稳定 API 错误码。
     *
     * @return 稳定 API 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
