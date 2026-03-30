package org.hunau.alert.model;

import lombok.Data;
import org.hunau.common.RiskLevel;

import java.time.LocalDateTime;

@Data
public class AlertRecord {
    private String eventId;
    private String qsId;
    private String companyId;
    private RiskLevel riskLevel;
    private double riskScore;
    private String detail;
    private String status;
    private String notifyChannel;
    private int notifyRetry;
    private LocalDateTime createdAt;
}

