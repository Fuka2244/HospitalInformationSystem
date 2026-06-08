package com.hospitalinfo.hospitalinformationsystem.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("SUCCESS", "操作成功"),
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败"),
    VALIDATION_ERROR("VALIDATION_ERROR", "参数校验失败"),
    BAD_REQUEST("BAD_REQUEST", "请求参数错误"),
    UNAUTHORIZED("UNAUTHORIZED", "请先登录"),
    FORBIDDEN("FORBIDDEN", "无权访问"),
    NOT_FOUND("NOT_FOUND", "资源不存在"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "请求方法不支持"),
    DATA_CONFLICT("DATA_CONFLICT", "数据冲突"),
    FILE_ERROR("FILE_ERROR", "文件处理失败"),
    DATABASE_ERROR("DATABASE_ERROR", "数据库操作失败"),
    CACHE_ERROR("CACHE_ERROR", "缓存服务异常"),
    MESSAGE_QUEUE_ERROR("MESSAGE_QUEUE_ERROR", "消息队列服务异常"),
    AI_SERVICE_ERROR("AI_SERVICE_ERROR", "AI服务暂时不可用"),
    SYSTEM_ERROR("SYSTEM_ERROR", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
