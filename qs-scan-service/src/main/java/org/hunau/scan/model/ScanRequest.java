package org.hunau.scan.model;

import lombok.Data;

@Data
public class ScanRequest {
    private String qsId;
    private String batchId;
    private String companyId;
    private String signature;
    private String signaturePayload;
    private String ip;
    private String deviceFingerprint;
    private String browser;
    private Double latitude;
    private Double longitude;
    private Double expectedLatitude;
    private Double expectedLongitude;
    private String locationSource;
}
