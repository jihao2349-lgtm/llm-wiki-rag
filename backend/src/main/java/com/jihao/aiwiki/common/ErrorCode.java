package com.jihao.aiwiki.common;

/**
 * v0.1 API 共享错误码。
 *
 * @author jihao
 * @date 2026/05/06
 */
public enum ErrorCode {

    /** 请求成功 */
    SUCCESS(200, "success"),

    /** 请求参数校验失败 */
    BAD_REQUEST(400, "bad request"),

    /** 请求资源不存在 */
    NOT_FOUND(404, "not found"),

    /** 请求与当前资源状态冲突 */
    CONFLICT(409, "conflict"),

    /** Vault 路径或文件系统操作不安全 */
    VAULT_PATH_UNSAFE(1001, "vault path unsafe"),

    /** Vault 项目未初始化 */
    VAULT_NOT_INITIALIZED(1002, "vault not initialized"),

    /** 资料解析失败 */
    SOURCE_PARSE_FAILED(2001, "source parse failed"),

    /** 摄入任务状态不允许当前操作 */
    TASK_STATE_INVALID(3001, "task state invalid"),

    /** 资料记录不存在 */
    SOURCE_NOT_FOUND(4041, "source not found"),

    /** URL 导入 SSRF 攻击被拒绝 */
    SOURCE_URL_SSRF(4031, "url ssrf blocked"),

    /** URL 格式无效 */
    SOURCE_URL_INVALID(4002, "url invalid"),

    /** Wiki 页面不存在 */
    WIKI_PAGE_NOT_FOUND(4042, "wiki page not found"),

    /** Wiki 路径不合法 */
    WIKI_PATH_FORBIDDEN(4032, "wiki path forbidden"),

    /** LLM Provider 调用失败 */
    LLM_CALL_FAILED(6001, "llm call failed"),

    /** 未预期的服务端异常 */
    INTERNAL_ERROR(500, "internal server error");

    /** 数字错误码 */
    private final Integer code;

    /** 默认错误消息 */
    private final String message;

    /**
     * 创建错误码枚举。
     *
     * @param code 数字错误码
     * @param message 默认错误消息
     */
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取数字错误码。
     *
     * @return 数字错误码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取默认错误消息。
     *
     * @return 默认错误消息
     */
    public String getMessage() {
        return message;
    }
}
