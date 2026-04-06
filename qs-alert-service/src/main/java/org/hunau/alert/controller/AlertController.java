package org.hunau.alert.controller;

import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.RuleStatusUpdateRequest;
import org.hunau.alert.model.RuleThresholdUpdateRequest;
import org.hunau.alert.service.AlertRuleEngineService;
import org.hunau.alert.service.AlertService;
import org.hunau.common.R;
import org.hunau.common.util.AssertUtil;
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

    @GetMapping("/list")
    public R<?> list() {
        return alertService.list();
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
}

