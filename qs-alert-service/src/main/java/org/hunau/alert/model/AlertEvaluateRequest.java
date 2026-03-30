package org.hunau.alert.model;

import lombok.Data;

@Data
public class AlertEvaluateRequest {
    private String qsId;
    private String companyId;
    private Integer scanCount;
    private Integer deviceCount;
    private Integer ipCount;
    private Double timeVariance;
    private Double locationVariance;
}

