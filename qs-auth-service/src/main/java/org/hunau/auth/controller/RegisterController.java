package org.hunau.auth.controller;

import org.hunau.auth.model.RegisterRequest;
import org.hunau.auth.service.RegisterService;
import org.hunau.common.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/auth/register", "/register"})
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/user")
    public R<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        try {
            String username = registerService.register(request);
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("registered", true);
            return R.ok(data);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }

    @PostMapping("/admin")
    public R<Map<String, Object>> registerAdmin(@RequestBody RegisterRequest request) {
        try {
            String username = registerService.registerAdmin(request);
            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("role", "ADMIN");
            data.put("registered", true);
            return R.ok(data);
        } catch (IllegalArgumentException ex) {
            return R.fail(ex.getMessage());
        }
    }
}

