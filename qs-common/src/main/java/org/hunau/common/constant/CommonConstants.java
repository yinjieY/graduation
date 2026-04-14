package org.hunau.common.constant;

public class CommonConstants {
    
    private CommonConstants() {
    }
    
    public static final String EMPTY_STRING = "";
    public static final String NULL_STRING = "null";
    
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 500;
    
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    public static final String BEARER_PREFIX = "Bearer ";
    
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_COMPANY = "COMPANY";
    public static final String ROLE_CONSUMER = "CONSUMER";
    
    public static final String LEVEL_HIGH = "HIGH";
    public static final String LEVEL_MEDIUM = "MEDIUM";
    public static final String LEVEL_LOW = "LOW";
}