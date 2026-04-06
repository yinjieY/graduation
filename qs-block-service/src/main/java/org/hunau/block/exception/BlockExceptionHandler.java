package org.hunau.block.exception;

import org.hunau.common.R;
import org.hunau.common.ResultCode;
import org.hunau.common.exception.BusinessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class BlockExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public R<?> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.NO_PERMISSION, "无权限访问该接口: " + e.getMessage());
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
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e) {
        return R.fail(ResultCode.ERROR, "服务器异常: " + e.getMessage());
    }
}

