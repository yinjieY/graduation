package org.hunau.scan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReusePatternScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReusePatternScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.feature.reuse.scan-update-lag-seconds:2}")
    private int scanUpdateLagSeconds;

    @Value("${app.feature.reuse.full-calibration-hour:3}")
    private int fullCalibrationHour;

    private boolean isFullCalibrationMode = false;

    public ReusePatternScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${app.feature.reuse.refresh-ms:86400000}",
            initialDelayString = "${app.feature.reuse.initial-delay-ms:15000}")
    public void scheduledRefreshReusePatterns() {
        int currentHour = LocalDateTime.now().getHour();
        isFullCalibrationMode = currentHour == fullCalibrationHour;
        refreshNow();
    }

    public int refreshNow() {
        return refreshNowWithMeta().affectedRows();
    }

    public RefreshSummary refreshNowWithMeta() {
        LocalDateTime snapshotTime = LocalDateTime.now().minusSeconds(Math.max(0, scanUpdateLagSeconds));
        
        if (isFullCalibrationMode) {
            return refreshFullCalibration(snapshotTime);
        } else {
            return refreshIncrementalWithFullIp(snapshotTime);
        }
    }

    private RefreshSummary refreshIncrementalWithFullIp(LocalDateTime snapshotTime) {
        String sql = """
                INSERT INTO yx_ai_feature.reuse_pattern
                (qs_id, scan_count, time_variance, location_variance, device_count, ip_count, is_reused, model_version, create_time, update_time)
                SELECT
                    s.qs_id,
                    COUNT(*) AS scan_count,
                    COALESCE(VAR_SAMP(TIMESTAMPDIFF(MINUTE, '2024-01-01 00:00:00', s.scan_time)) / 1440, 0) AS time_variance,
                    COALESCE(VAR_SAMP(s.lat), 0) + COALESCE(VAR_SAMP(s.lng), 0) AS location_variance,
                    COUNT(DISTINCT s.device_fingerprint) AS device_count,
                    COUNT(DISTINCT s.ip_masked) AS ip_count,
                    COALESCE(rp.is_reused, 0) AS is_reused,
                    'v1.0' AS model_version,
                    COALESCE(rp.create_time, NOW()) AS create_time,
                    NOW() AS update_time
                FROM yx_scan_anomaly.scan_log s
                LEFT JOIN yx_ai_feature.reuse_pattern rp ON rp.qs_id = s.qs_id
                WHERE s.scan_time <= ?
                GROUP BY s.qs_id, rp.is_reused, rp.create_time
                ON DUPLICATE KEY UPDATE
                    scan_count = VALUES(scan_count),
                    time_variance = VALUES(time_variance),
                    location_variance = VALUES(location_variance),
                    device_count = VALUES(device_count),
                    ip_count = VALUES(ip_count),
                    is_reused = VALUES(is_reused),
                    model_version = VALUES(model_version),
                    update_time = VALUES(update_time)
                """;

        int affected = jdbcTemplate.update(sql, snapshotTime);
        log.info("[增量统计] AI特征已同步更新, qsIds={}, snapshotTime={}", affected, snapshotTime);
        return new RefreshSummary(affected, snapshotTime.toString(), scanUpdateLagSeconds, false);
    }

    private RefreshSummary refreshFullCalibration(LocalDateTime snapshotTime) {
        String sql = """
                INSERT INTO yx_ai_feature.reuse_pattern
                (qs_id, scan_count, time_variance, location_variance, device_count, ip_count, is_reused, model_version, create_time, update_time)
                SELECT
                    s.qs_id,
                    COUNT(*) AS scan_count,
                    COALESCE(VAR_SAMP(TIMESTAMPDIFF(MINUTE, '2024-01-01 00:00:00', s.scan_time)) / 1440, 0) AS time_variance,
                    COALESCE(VAR_SAMP(s.lat), 0) + COALESCE(VAR_SAMP(s.lng), 0) AS location_variance,
                    COUNT(DISTINCT s.device_fingerprint) AS device_count,
                    COUNT(DISTINCT s.ip_masked) AS ip_count,
                    COALESCE(rp.is_reused, 0) AS is_reused,
                    'v1.0' AS model_version,
                    COALESCE(rp.create_time, NOW()) AS create_time,
                    NOW() AS update_time
                FROM yx_scan_anomaly.scan_log s
                LEFT JOIN yx_ai_feature.reuse_pattern rp ON rp.qs_id = s.qs_id
                WHERE s.scan_time <= ?
                GROUP BY s.qs_id, rp.is_reused, rp.create_time
                ON DUPLICATE KEY UPDATE
                    scan_count = VALUES(scan_count),
                    time_variance = VALUES(time_variance),
                    location_variance = VALUES(location_variance),
                    device_count = VALUES(device_count),
                    ip_count = VALUES(ip_count),
                    is_reused = VALUES(is_reused),
                    model_version = VALUES(model_version),
                    update_time = VALUES(update_time)
                """;

        int affected = jdbcTemplate.update(sql, snapshotTime);
        log.info("[全量校准] AI特征全量重算完成, qsIds={}, snapshotTime={}", affected, snapshotTime);
        return new RefreshSummary(affected, snapshotTime.toString(), scanUpdateLagSeconds, true);
    }

    @Scheduled(cron = "${app.feature.reuse.daily-calibration-cron:0 0 3 * * ?}")
    public void scheduledDailyFullCalibration() {
        isFullCalibrationMode = true;
        try {
            RefreshSummary summary = refreshNowWithMeta();
            log.info("[每日全量校准] 执行完成, affectedRows={}, snapshotTime={}", 
                    summary.affectedRows(), summary.snapshotTime());
        } finally {
            isFullCalibrationMode = false;
        }
    }

    public record RefreshSummary(int affectedRows,
                                 String snapshotTime,
                                 int scanUpdateLagSeconds,
                                 boolean isFullCalibration) {
    }
}
