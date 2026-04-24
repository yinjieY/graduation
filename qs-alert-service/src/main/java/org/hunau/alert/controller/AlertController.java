package org.hunau.alert.controller;

import org.hunau.alert.model.AdminActionNotificationRequest;
import org.hunau.alert.model.AlertEvaluateRequest;
import org.hunau.alert.model.FeedbackMessageRequest;
import org.hunau.alert.model.RuleScoreWeightUpdateRequest;
import org.hunau.alert.model.RuleStatusUpdateRequest;
import org.hunau.alert.model.RuleThresholdUpdateRequest;
import org.hunau.alert.service.AdminActionNotificationService;
import org.hunau.alert.service.AlertRuleEngineService;
import org.hunau.alert.service.AlertService;
import org.hunau.alert.service.NotificationGateway;
import org.hunau.alert.model.AlertRecord;
import org.hunau.common.enums.RiskLevel;
import org.hunau.common.model.R;
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
    private final AdminActionNotificationService adminActionNotificationService;
    private final NotificationGateway notificationGateway;

    public AlertController(AlertService alertService, AlertRuleEngineService alertRuleEngineService,
                          AdminActionNotificationService adminActionNotificationService,
                          NotificationGateway notificationGateway) {
        this.alertService = alertService;
        this.alertRuleEngineService = alertRuleEngineService;
        this.adminActionNotificationService = adminActionNotificationService;
        this.notificationGateway = notificationGateway;
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

    @GetMapping("/unread/count")
    public R<?> countUnread(@RequestParam(value = "companyId", required = false) String companyIdParam,
                            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.countUnread(companyId);
    }

    @PostMapping("/{alertId}/read")
    public R<?> markAsRead(@PathVariable Long alertId,
                           @RequestParam(value = "companyId", required = false) String companyIdParam,
                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.markAsRead(alertId, companyId);
    }

    @PostMapping("/read/all")
    public R<?> markAllAsRead(@RequestParam(value = "companyId", required = false) String companyIdParam,
                              @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.markAllAsRead(companyId);
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

    @PutMapping("/rules/{ruleId}/weight")
    public R<?> updateRuleScoreWeight(@PathVariable String ruleId,
                                      @RequestBody RuleScoreWeightUpdateRequest request) {
        AssertUtil.notEmpty(ruleId, "ruleId不能为空");
        AssertUtil.notNull(request, "请求不能为空");
        AssertUtil.notNull(request.getScoreWeight(), "scoreWeight不能为空");
        if (request.getScoreWeight() < 0 || request.getScoreWeight() > 1) {
            return R.fail("scoreWeight必须在0到1之间");
        }
        alertRuleEngineService.updateScoreWeight(ruleId, request.getScoreWeight());
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

    @GetMapping("/system-notifications")
    public R<?> listSystemNotifications(@RequestParam(value = "companyId", required = false) String companyIdParam,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.listSystemNotifications(companyId);
    }

    @PostMapping("/system-notifications/{notificationId}/read")
    public R<?> markNotificationAsRead(@PathVariable Long notificationId,
                                       @RequestParam(value = "companyId", required = false) String companyIdParam,
                                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.markNotificationAsRead(notificationId, companyId);
    }

    @PostMapping("/system-notifications/read/all")
    public R<?> markAllNotificationsAsRead(@RequestParam(value = "companyId", required = false) String companyIdParam,
                                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.markAllNotificationsAsRead(companyId);
    }

    @GetMapping("/system-notifications/unread/count")
    public R<?> countUnreadNotifications(@RequestParam(value = "companyId", required = false) String companyIdParam,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        String companyId = companyIdParam != null ? companyIdParam.trim() : resolveCompanyId(authorization);
        return alertService.countUnreadNotifications(companyId);
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

    @PostMapping("/admin/action/notify")
    public R<?> sendAdminActionNotification(@RequestBody AdminActionNotificationRequest request) {
        return adminActionNotificationService.sendNotification(request);
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

    @GetMapping("/test/mail")
    @org.springframework.security.access.prepost.PreAuthorize("permitAll()")
    public R<?> testMailNotification(@RequestParam(required = false) String to) {
        AlertRecord testRecord = new AlertRecord();
        testRecord.setEventId("TEST-" + System.currentTimeMillis());
        testRecord.setQsId("QS-TEST-001");
        testRecord.setCompanyId("C-TEST-001");
        testRecord.setRiskScore(0.75);
        testRecord.setRiskLevel(RiskLevel.HIGH);
        testRecord.setDetail("这是一封测试邮件，用于验证邮件推送系统是否正常工作。\n测试内容：邮件发送功能测试\n时间：" + java.time.LocalDateTime.now());

        NotificationGateway.NotificationResult result = notificationGateway.send(testRecord, to);
        
        return R.ok(java.util.Map.of(
                "success", result.success(),
                "retryCount", result.retryCount(),
                "pushStatus", result.pushStatus(),
                "message", result.message(),
                "channelSummary", result.channelSummary()
        ));
    }
}

