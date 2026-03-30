package org.hunau.scan.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScanLog {
    private String qsId;
    private String batchId;
    private String companyId;
    private LocalDateTime scanTime;
    private String maskedPhone;
    private String maskedIp;
    private String deviceFingerprint;
    private String os;
    private String browser;
    private Double latitude;
    private Double longitude;
    private boolean firstScan;
    private boolean crossRegionRisk;
}

