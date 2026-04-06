package org.hunau.auth.controller;

import org.hunau.auth.model.CompanyApplyRequest;
import org.hunau.auth.model.CompanyReviewRequest;
import org.hunau.auth.service.CompanyAuthService;
import org.hunau.common.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        Map<String, Object> data = companyAuthService.queryStatus(normalize(companyId));
        if (data == null) {
            return R.fail("企业认证记录不存在");
        }
        return R.ok(data);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('COMPANY','ADMIN')")
    public R<Map<String, Object>> apply(@RequestBody CompanyApplyRequest request,
                                        Authentication authentication) {
        String companyId = resolveApplyCompanyId(request, authentication);
        String companyName = normalize(request.getCompanyName());
        if (companyId.isEmpty() || companyName.isEmpty()) {
            return R.fail("companyId 和 companyName 不能为空");
        }
        Map<String, Object> data = companyAuthService.submit(companyId, companyName, request.getRemark());
        return R.ok(data);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public R<List<Map<String, Object>>> pending() {
        return R.ok(companyAuthService.pendingList());
    }

    @PutMapping("/review/{companyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> review(@PathVariable String companyId,
                                         @RequestBody CompanyReviewRequest request,
                                         Authentication authentication) {
        Boolean approved = request.getApproved();
        if (approved == null) {
            return R.fail("approved 不能为空");
        }

        String normalizedCompanyId = normalize(companyId);
        if (normalizedCompanyId.isEmpty()) {
            return R.fail("companyId 不能为空");
        }

        String normalizedCompanyName = normalize(request.getCompanyName());
        if (normalizedCompanyName.isEmpty()) {
            Map<String, Object> old = companyAuthService.queryStatus(normalizedCompanyId);
            normalizedCompanyName = old == null ? normalizedCompanyId : String.valueOf(old.getOrDefault("companyName", normalizedCompanyId));
        }

        String reviewer = authentication == null ? "" : normalize(authentication.getName());
        Map<String, Object> data = companyAuthService.review(
                normalizedCompanyId,
                normalizedCompanyName,
                approved,
                reviewer,
                request.getRemark()
        );
        return R.ok(data);
    }

    // Backward compatible endpoint for old callers: /review/{companyId}?approved=true
    @PutMapping(value = "/review/{companyId}", params = "approved")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Map<String, Object>> reviewCompat(@PathVariable String companyId,
                                                @RequestParam boolean approved,
                                                Authentication authentication) {
        CompanyReviewRequest request = new CompanyReviewRequest();
        request.setApproved(approved);
        return review(companyId, request, authentication);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveApplyCompanyId(CompanyApplyRequest request, Authentication authentication) {
        String requestCompanyId = normalize(request.getCompanyId());
        if (authentication == null) {
            return requestCompanyId;
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (isAdmin) {
            return requestCompanyId;
        }

        String username = normalize(authentication.getName());
        return companyAuthService.findCompanyIdByUsername(username);
    }

}
