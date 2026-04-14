package org.hunau.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.hunau.auth.model.RegisterRequest;
import org.hunau.auth.service.RegisterService;
import org.hunau.common.model.R;
import org.hunau.common.enums.ResultCode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/auth/register", "/register"})
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/user")
    public R<Map<String, Object>> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            return R.ok(registerService.register(request));
        } catch (IllegalArgumentException ex) {
            response.setStatus(ResultCode.PARAM_ERROR.getCode());
            return R.fail(ResultCode.PARAM_ERROR, ex.getMessage());
        }
    }

    @PostMapping("/admin")
    public R<Map<String, Object>> registerAdmin(@RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            return R.ok(registerService.registerAdmin(request));
        } catch (IllegalArgumentException ex) {
            response.setStatus(ResultCode.PARAM_ERROR.getCode());
            return R.fail(ResultCode.PARAM_ERROR, ex.getMessage());
        }
    }
}