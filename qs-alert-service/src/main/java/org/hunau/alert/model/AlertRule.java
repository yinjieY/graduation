package org.hunau.alert.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertRule {
    private String ruleId;
    private String ruleName;
    private String ruleContent;
    private String threshold;
    private Double scoreWeight;
    private Integer alertLevel;
    private Integer status;
    private LocalDateTime updateTime;
}

