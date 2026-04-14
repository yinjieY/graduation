package org.hunau.alert.model;

public enum AdminActionType {
    
    COMPANY_AUTH_APPROVED("企业认证审核通过"),
    COMPANY_AUTH_REJECTED("企业认证审核拒绝"),
    COMPANY_STATUS_CHANGED("企业状态变更"),
    COMPANY_LEVEL_CHANGED("企业等级变更"),
    BATCH_REVIEW_APPROVED("批次审核通过"),
    BATCH_REVIEW_REJECTED("批次审核拒绝"),
    QS_CODE_FROZEN("二维码冻结"),
    QS_CODE_UNFROZEN("二维码解冻"),
    MANUAL_ALERT_CREATED("人工预警创建"),
    SYSTEM_NOTICE("系统通知");

    private final String description;

    AdminActionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}