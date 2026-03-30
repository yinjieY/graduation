package org.hunau.common;

import lombok.Data;

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
        r.setCode(ResultCode.SUCCESS);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail() {
        return fail(ResultCode.ERROR, "操作失败");
    }

    public static <T> R<T> fail(String msg) {
        return fail(ResultCode.ERROR, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
