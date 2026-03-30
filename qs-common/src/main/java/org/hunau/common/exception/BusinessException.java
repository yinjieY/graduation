package org.hunau.common.exception;

import lombok.Data;
import org.hunau.common.ResultCode;

@Data
public class BusinessException extends RuntimeException {
    private int code;
    private String msg;

    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.BUSINESS_ERROR;
        this.msg = msg;
    }
}
