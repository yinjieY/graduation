package org.hunau.scan.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScanLog {
    private String qsId;
    private String batchId;
    private String companyId;
    private LocalDateTime scanTime;
    private String maskedIp;
    private String deviceFingerprint;
    private String browser;
    private Double latitude;
    private Double longitude;
    private String locationSource;
    private Double distanceKm;
    private boolean firstScan;
    private boolean crossRegionRisk;
    private boolean newDevice;
    private boolean riskDevice;
    private String city;
    private String province;
}
