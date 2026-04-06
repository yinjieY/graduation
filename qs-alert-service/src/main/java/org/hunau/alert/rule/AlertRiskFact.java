package org.hunau.alert.rule;

import java.util.ArrayList;
import java.util.List;

public class AlertRiskFact {

    private final int scanCount1h;
    private final int deviceCount1d;
    private final int ipCount1h;
    private final List<String> hitRuleIds = new ArrayList<>();
    private final List<String> hitReasons = new ArrayList<>();
    private double ruleScore;

    public AlertRiskFact(int scanCount1h, int deviceCount1d, int ipCount1h) {
        this.scanCount1h = scanCount1h;
        this.deviceCount1d = deviceCount1d;
        this.ipCount1h = ipCount1h;
        this.ruleScore = 0.0;
    }

    public int getScanCount1h() {
        return scanCount1h;
    }

    public int getDeviceCount1d() {
        return deviceCount1d;
    }

    public int getIpCount1h() {
        return ipCount1h;
    }

    public double getRuleScore() {
        return ruleScore;
    }

    public List<String> getHitRuleIds() {
        return hitRuleIds;
    }

    public List<String> getHitReasons() {
        return hitReasons;
    }

    public void increaseRuleScore(double delta) {
        if (delta <= 0) {
            return;
        }
        this.ruleScore = Math.min(1.0, this.ruleScore + delta);
    }

    public void addHit(String ruleId, String reason) {
        if (ruleId != null && !ruleId.isBlank()) {
            hitRuleIds.add(ruleId);
        }
        if (reason != null && !reason.isBlank()) {
            hitReasons.add(reason);
        }
    }
}

