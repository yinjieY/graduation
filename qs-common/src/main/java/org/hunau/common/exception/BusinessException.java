package org.hunau.common.exception;

import lombok.Data;
import org.hunau.common.enums.ResultCode;

@Data
public class BusinessException extends RuntimeException {
    private int code;
    private String msg;

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.msg = msg;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.msg = resultCode.getMessage();
    }
}