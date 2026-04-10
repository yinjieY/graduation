package org.hunau.alert.controller;

import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.FeedbackMessageRequest;
import org.hunau.alert.model.RuleStatusUpdateRequest;
import org.hunau.alert.model.RuleThresholdUpdateRequest;
import org.hunau.alert.service.AlertRuleEngineService;
import org.hunau.alert.service.AlertService;
import org.hunau.common.R;
import org.hunau.common.util.AssertUtil;
import org.hunau.common.util.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alert")
public class AlertController {

    private final AlertService alertService;
    private final AlertRuleEngineService alertRuleEngineService;

    public AlertController(AlertService alertService, AlertRuleEngineService alertRuleEngineService) {
        this.alertService = alertService;
        this.alertRuleEngineService = alertRuleEngineService;
    }

    @PostMapping("/evaluate")
    public R<?> evaluate(@RequestBody AlertEvaluateRequest request) {
        return alertService.evaluate(request);
    }

    @GetMapping({"/list", "/messages"})
    public R<?> list(@RequestParam(value = "companyId", required = false) String companyIdParam,
                     @RequestHeader(value = "Authorization", required = false) String authorization,
                     Authentication authentication) {
        String role = resolveRole(authentication);
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.list(role, companyId);
    }

    @PostMapping("/messages/feedback")
    public R<?> createFeedbackMessage(@RequestBody FeedbackMessageRequest request) {
        return alertService.createFeedbackMessage(request);
    }

    @GetMapping("/rules")
    public R<?> listRules() {
        return R.ok(alertRuleEngineService.listRules());
    }

    @PutMapping("/rules/{ruleId}/threshold")
    public R<?> updateRuleThreshold(@PathVariable String ruleId,
                                    @RequestBody RuleThresholdUpdateRequest request) {
        AssertUtil.notEmpty(ruleId, "ruleId不能为空");
        AssertUtil.notNull(request, "请求不能为空");
        AssertUtil.notEmpty(request.getThreshold(), "threshold不能为空");
        alertRuleEngineService.updateThreshold(ruleId, request.getThreshold().trim());
        return R.ok(alertRuleEngineService.reloadNow());
    }

    @PutMapping("/rules/{ruleId}/status")
    public R<?> updateRuleStatus(@PathVariable String ruleId,
                                 @RequestBody RuleStatusUpdateRequest request) {
        AssertUtil.notEmpty(ruleId, "ruleId不能为空");
        AssertUtil.notNull(request, "请求不能为空");
        AssertUtil.notNull(request.getStatus(), "status不能为空");
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            return R.fail("status仅支持0或1");
        }
        alertRuleEngineService.updateStatus(ruleId, request.getStatus());
        return R.ok(alertRuleEngineService.reloadNow());
    }

    @PostMapping("/rules/reload")
    public R<?> reloadRules() {
        return R.ok(alertRuleEngineService.reloadNow());
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String item = authority.getAuthority();
            if (item != null && item.startsWith("ROLE_")) {
                return item.substring(5);
            }
        }
        return "";
    }

    private String resolveCompanyId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        String token = authorization.substring(7);
        if (!JwtUtil.validate(token)) {
            return "";
        }
        String companyId = JwtUtil.getCompanyId(token);
        return companyId == null ? "" : companyId.trim();
    }
}

