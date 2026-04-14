package org.hunau.common.enums;

public enum UserRole {
    ADMIN("管理员"),
    COMPANY("企业用户");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}