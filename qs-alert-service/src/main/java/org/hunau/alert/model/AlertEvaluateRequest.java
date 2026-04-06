package org.hunau.alert.model;

import lombok.Data;

@Data
public class AlertEvaluateRequest {
    private String qsId;
    private String companyId;
    // Canonical counters consumed by the Drools rules.
    private Integer scanCount1h;
    private Integer deviceCount1d;
    private Integer ipCount1h;

    // Backward-compatible counters for existing callers.
    private Integer scanCount;
    private Integer deviceCount;
    private Integer ipCount;
    private Double timeVariance;
    private Double locationVariance;
    private Boolean newDevice;
    private Boolean riskDevice;
    private Double distanceKm;
    private String city;
    private String province;
}
