package org.hunau.common.model;

import lombok.Data;
import org.hunau.common.enums.ResultCode;

import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage());
    }

    public static <T> R<T> fail(String msg) {
        return fail(ResultCode.ERROR.getCode(), msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
    
    public static <T> R<T> fail(ResultCode code) {
        R<T> r = new R<>();
        r.setCode(code.getCode());
        r.setMsg(code.getMessage());
        return r;
    }

    public static <T> R<T> fail(ResultCode code, String msg) {
        R<T> r = new R<>();
        r.setCode(code.getCode());
        r.setMsg(msg);
        return r;
    }
    
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}