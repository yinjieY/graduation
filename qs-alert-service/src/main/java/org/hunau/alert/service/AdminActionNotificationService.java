package org.hunau.alert.service;

import org.hunau.alert.model.AdminActionNotificationRequest;
import org.hunau.common.model.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AdminActionNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminActionNotificationService.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminActionNotificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public R<Map<String, Object>> sendNotification(AdminActionNotificationRequest request) {
        if (request.getCompanyId() == null || request.getCompanyId().trim().isEmpty()) {
            return R.fail("companyId 不能为空");
        }
        if (request.getActionType() == null || request.getActionType().trim().isEmpty()) {
            return R.fail("actionType 不能为空");
        }
        if (request.getActionContent() == null || request.getActionContent().trim().isEmpty()) {
            return R.fail("actionContent 不能为空");
        }

        String companyId = request.getCompanyId().trim();
        String actionType = request.getActionType().trim();
        
        String title = buildTitle(actionType, request.getCompanyName());
        String content = buildContent(request);

        return insertNotification(companyId, actionType, title, content, request.getSourceModule(), request.getSourceId());
    }
    
    public R<Map<String, Object>> sendNotification(Map<String, Object> body) {
        String companyId = (String) body.get("companyId");
        String actionType = (String) body.get("actionType");
        String actionContent = (String) body.get("actionContent");
        
        if (companyId == null || companyId.trim().isEmpty()) {
            return R.fail("companyId 不能为空");
        }
        if (actionType == null || actionType.trim().isEmpty()) {
            return R.fail("actionType 不能为空");
        }
        if (actionContent == null || actionContent.trim().isEmpty()) {
            return R.fail("actionContent 不能为空");
        }

        String companyName = (String) body.get("companyName");
        String sourceModule = (String) body.get("sourceModule");
        String sourceId = (String) body.get("sourceId");
        String operator = (String) body.get("operator");
        
        String title = buildTitle(actionType, companyName);
        String content = buildContent(actionType, actionContent, operator, sourceModule, sourceId);

        return insertNotification(companyId, actionType, title, content, sourceModule, sourceId);
    }
    
    private R<Map<String, Object>> insertNotification(String companyId, String actionType, String title, 
                                                       String content, String sourceModule, String sourceId) {
        jdbcTemplate.update(
                "INSERT INTO sys_notification(company_id, title, content, type, source_module, source_id, status, created_at) " +
                "VALUES (?, ?, ?, 'OPERATION', ?, ?, 'UNREAD', NOW())",
                companyId,
                title,
                content,
                sourceModule,
                sourceId
        );

        Long notificationId = jdbcTemplate.queryForObject(
                "SELECT notification_id FROM sys_notification WHERE company_id = ? ORDER BY notification_id DESC LIMIT 1",
                Long.class,
                companyId
        );

        log.info("管理员操作通知已发送: companyId={}, actionType={}, notificationId={}", 
                companyId, actionType, notificationId);

        return R.ok(Map.of(
                "notificationId", notificationId,
                "companyId", companyId,
                "created", notificationId != null
        ));
    }

    private String buildTitle(String actionType, String companyName) {
        String companyPrefix = companyName != null && !companyName.isBlank() ? "【" + companyName + "】" : "";
        
        return switch (actionType) {
            case "COMPANY_AUTH_APPROVED" -> companyPrefix + "企业认证审核通过";
            case "COMPANY_AUTH_REJECTED" -> companyPrefix + "企业认证审核未通过";
            case "COMPANY_STATUS_CHANGED" -> companyPrefix + "企业状态变更通知";
            case "COMPANY_LEVEL_CHANGED" -> companyPrefix + "企业等级调整通知";
            case "BATCH_REVIEW_APPROVED" -> companyPrefix + "批次审核通过";
            case "BATCH_REVIEW_REJECTED" -> companyPrefix + "批次审核未通过";
            case "QS_CODE_FROZEN" -> companyPrefix + "二维码已冻结";
            case "QS_CODE_UNFROZEN" -> companyPrefix + "二维码已解冻";
            case "MANUAL_ALERT_CREATED" -> companyPrefix + "人工预警通知";
            default -> companyPrefix + "系统操作通知";
        };
    }

    private String buildContent(AdminActionNotificationRequest request) {
        StringBuilder content = new StringBuilder();
        
        content.append(request.getActionContent());
        
        if (request.getOperator() != null && !request.getOperator().isBlank() && !"ADMIN".equals(request.getOperator())) {
            content.append("，操作人：").append(request.getOperator());
        }
        
        content.append("。");
        
        return content.toString();
    }
    
    private String buildContent(String actionType, String actionContent, String operator, 
                               String sourceModule, String sourceId) {
        StringBuilder content = new StringBuilder();
        
        content.append(actionContent);
        
        if (operator != null && !operator.isBlank() && !"ADMIN".equals(operator)) {
            content.append("，操作人：").append(operator);
        }
        
        content.append("。");
        
        return content.toString();
    }
}