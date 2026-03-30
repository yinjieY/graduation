package org.hunau.common.exception;

import org.hunau.common.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e) {
        e.printStackTrace();
        return R.fail("服务器异常：" + e.getMessage());
    }
}
