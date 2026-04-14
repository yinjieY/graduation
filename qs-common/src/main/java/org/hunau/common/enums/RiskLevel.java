package org.hunau.common.enums;

public enum RiskLevel {
    CRITICAL("极高风险", 4, 86, 100),
    HIGH("高风险", 3, 61, 85),
    MEDIUM("中风险", 2, 31, 60),
    LOW("低风险", 1, 0, 30);
    
    private final String description;
    private final int level;
    private final int minScore;
    private final int maxScore;
    
    RiskLevel(String description, int level, int minScore, int maxScore) {
        this.description = description;
        this.level = level;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getMinScore() {
        return minScore;
    }
    
    public int getMaxScore() {
        return maxScore;
    }
    
    public boolean isScoreInRange(double score) {
        return score >= minScore && score <= maxScore;
    }
    
    public static RiskLevel fromScore(double score) {
        if (score >= CRITICAL.minScore) {
            return CRITICAL;
        } else if (score >= HIGH.minScore) {
            return HIGH;
        } else if (score >= MEDIUM.minScore) {
            return MEDIUM;
        } else {
            return LOW;
        }
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
    
    public boolean shouldAutoFreeze() {
        return this == CRITICAL;
    }
}