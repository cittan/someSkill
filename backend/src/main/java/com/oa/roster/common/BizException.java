package com.oa.roster.common;

import lombok.Getter;

/**
 * 业务异常：code 同时作为 HTTP 状态码返回（限 4xx/5xx），便于前端区分"未登录/无权限/业务错误"。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(403, message);
    }
}
