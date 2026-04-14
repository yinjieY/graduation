package org.hunau.common.enums;

public enum RiskLevel {
    HIGH("高风险", 3),
    MEDIUM("中风险", 2),
    LOW("低风险", 1);
    
    private final String description;
    private final int level;
    
    RiskLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public static RiskLevel fromString(String value) {
        if (value == null) {
            return LOW;
        }
        try {
            return RiskLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}