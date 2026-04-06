package org.hunau.scan.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceProfile {
    private String deviceFingerprint;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private String city;
    private String province;
    private String os;
    private String browser;
    private boolean risk;
    private int scanCount;
}

