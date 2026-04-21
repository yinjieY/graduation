package org.hunau.alert.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hunau.common.enums.RiskLevel;

import java.time.LocalDateTime;

@Data
public class AlertRecord {
    private String eventId;
    private String qsId;
    private String companyId;
    private String batchId;
    private String batchName;
    
    @JsonIgnore
    private RiskLevel riskLevel;
    
    @JsonProperty("riskLevel")
    public String getRiskLevelString() {
        return riskLevel != null ? riskLevel.name() : null;
    }
    
    private double riskScore;
    private String detail;
    private String status;
    private String notifyChannel;
    private int notifyRetry;
    private LocalDateTime notifyTime;
    private LocalDateTime createdAt;
}

