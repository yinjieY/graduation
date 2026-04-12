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

import jakarta.servlet.http.HttpServletResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusiness(BusinessException e, HttpServletResponse response) {
        response.setStatus(e.getCode());
        return R.fail(e.getCode(), e.getMsg());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e, HttpServletResponse response) {
        response.setStatus(ResultCode.PARAM_ERROR);
        return R.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<?> handleMissingParam(MissingServletRequestParameterException e, HttpServletResponse response) {
        response.setStatus(ResultCode.PARAM_ERROR);
        return R.fail(ResultCode.PARAM_ERROR, "缺少请求参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletResponse response) {
        response.setStatus(ResultCode.PARAM_ERROR);
        return R.fail(ResultCode.PARAM_ERROR, "参数类型错误: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleBodyNotReadable(HttpMessageNotReadableException e, HttpServletResponse response) {
        response.setStatus(ResultCode.PARAM_ERROR);
        return R.fail(ResultCode.PARAM_ERROR, "请求体格式错误，请检查 JSON 字段和类型: " + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletResponse response) {
        response.setStatus(ResultCode.PARAM_ERROR);
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_ERROR, msg.isEmpty() ? "参数校验失败" : msg);
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleAll(Exception e, HttpServletResponse response) {
        response.setStatus(ResultCode.ERROR);
        return R.fail(ResultCode.ERROR, "服务器异常，请联系管理员。详细信息: " + e.getMessage());
    }
}
