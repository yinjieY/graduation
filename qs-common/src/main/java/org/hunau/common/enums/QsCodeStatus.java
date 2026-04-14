package org.hunau.common.enums;

public enum QsCodeStatus {
    ACTIVE("活跃"),
    INVALID("已停用"),
    FROZEN("已冻结"),
    CANCELLED("已作废");
    
    private final String description;
    
    QsCodeStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static QsCodeStatus fromString(String value) {
        if (value == null) {
            return ACTIVE;
        }
        try {
            return QsCodeStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ACTIVE;
        }
    }
}