package org.hunau.auth.controller;

import org.hunau.auth.service.CompanyAuthService;
import org.hunau.common.R;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/company")
public class CompanyAuthController {

    private final CompanyAuthService companyAuthService;

    public CompanyAuthController(CompanyAuthService companyAuthService) {
        this.companyAuthService = companyAuthService;
    }

    @GetMapping("/status/{companyId}")
    public R<Map<String, Object>> status(@PathVariable String companyId) {
        boolean approved = companyAuthService.isApproved(companyId);
        Map<String, Object> data = new HashMap<>();
        data.put("companyId", companyId);
        data.put("approved", approved);
        return R.ok(data);
    }

    @PutMapping("/review/{companyId}")
    public R<Map<String, Object>> review(@PathVariable String companyId,
                                         @RequestParam boolean approved) {
        boolean newStatus = companyAuthService.review(companyId, approved);
        Map<String, Object> data = new HashMap<>();
        data.put("companyId", companyId);
        data.put("approved", newStatus);
        return R.ok(data);
    }
}

