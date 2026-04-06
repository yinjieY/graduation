package org.hunau.common.exception;

import org.hunau.common.R;
import org.hunau.common.ResultCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(ResultCode.PARAM_ERROR, e.getMessage());
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
    public R<?> handleBodyNotReadable(HttpMessageNotReadableException e) {
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误，请检查 JSON 字段和类型: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_ERROR, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e) {
        return R.fail(ResultCode.ERROR, "服务器异常，请联系管理员。详细信息: " + e.getMessage());
    }
}
