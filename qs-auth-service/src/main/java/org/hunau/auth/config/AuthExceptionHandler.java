package org.hunau.auth.config;

import org.hunau.common.R;
import org.hunau.common.ResultCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public R<?> handleUsernameNotFound(UsernameNotFoundException e) {
        return R.fail(ResultCode.NO_AUTH, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<?> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.NO_PERMISSION, "权限不足，无法访问该接口: " + e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.PARAM_ERROR, "缺少请求参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(ResultCode.PARAM_ERROR, "参数类型错误: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleNotReadable(HttpMessageNotReadableException e) {
        String detail = e.getMostSpecificCause().getMessage();
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误: " + detail);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleOther(Exception e) {
        return R.fail(ResultCode.ERROR, "服务器异常: " + e.getMessage());
    }
}


