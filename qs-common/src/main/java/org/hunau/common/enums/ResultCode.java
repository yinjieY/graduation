package org.hunau.common.enums;

public enum ResultCode {
    SUCCESS(200, "操作成功"),
    ERROR(500, "服务器异常"),
    PARAM_ERROR(400, "参数错误"),
    NO_PERMISSION(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    UNAUTHORIZED(401, "未授权"),
    BUSINESS_ERROR(1000, "业务异常");
    
    private final int code;
    private final String message;
    
    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}