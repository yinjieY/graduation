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

    @Value("${app.feature.reuse.lookback-minutes:1440}")
    private int lookbackMinutes;

    @Value("${app.feature.reuse.scan-update-lag-seconds:2}")
    private int scanUpdateLagSeconds;

    @Value("${app.feature.reuse.min-update-gap-seconds:1}")
    private int minUpdateGapSeconds;

    public ReusePatternScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Periodically aggregate scan logs into reuse_pattern to avoid heavy per-scan calculations.
    @Scheduled(fixedDelayString = "${app.feature.reuse.refresh-ms:86400000}",
            initialDelayString = "${app.feature.reuse.initial-delay-ms:15000}")
    public void scheduledRefreshReusePatterns() {
        refreshNow();
    }

    public int refreshNow() {
        return refreshNowWithMeta().affectedRows();
    }

    public RefreshSummary refreshNowWithMeta() {
        LocalDateTime snapshotTime = LocalDateTime.now().minusSeconds(Math.max(0, scanUpdateLagSeconds));
        int effectiveMinGapSeconds = Math.max(0, minUpdateGapSeconds);
        String sql = """
                INSERT INTO yx_ai_feature.reuse_pattern
                (qs_id, scan_count, time_variance, location_variance, device_count, ip_count, is_reused, model_version, create_time, update_time)
                SELECT
                    s.qs_id,
                    COUNT(*) AS scan_count,
                    COALESCE(VAR_POP(TIMESTAMPDIFF(SECOND, '1970-01-01 00:00:00', s.scan_time)), 0) AS time_variance,
                    COALESCE(VAR_POP(s.lat), 0) + COALESCE(VAR_POP(s.lng), 0) AS location_variance,
                    COUNT(DISTINCT s.device_fingerprint) AS device_count,
                    COUNT(DISTINCT s.ip_masked) AS ip_count,
                    0 AS is_reused,
                    'v1.0' AS model_version,
                    NOW() AS create_time,
                    NOW() AS update_time
                FROM yx_scan_anomaly.scan_log s
                INNER JOIN (
                    SELECT qs_id, MAX(scan_time) AS latest_scan_time
                    FROM yx_scan_anomaly.scan_log
                    WHERE scan_time <= ?
                      AND scan_time >= DATE_SUB(?, INTERVAL ? MINUTE)
                    GROUP BY qs_id
                ) latest ON latest.qs_id = s.qs_id
                LEFT JOIN yx_ai_feature.reuse_pattern rp ON rp.qs_id = s.qs_id
                WHERE s.scan_time <= ?
                  AND s.scan_time >= DATE_SUB(?, INTERVAL ? MINUTE)
                  AND (rp.update_time IS NULL OR TIMESTAMPDIFF(SECOND, rp.update_time, latest.latest_scan_time) > ?)
                GROUP BY s.qs_id
                ON DUPLICATE KEY UPDATE
                    scan_count = VALUES(scan_count),
                    time_variance = VALUES(time_variance),
                    location_variance = VALUES(location_variance),
                    device_count = VALUES(device_count),
                    ip_count = VALUES(ip_count),
                    model_version = VALUES(model_version),
                    update_time = VALUES(update_time)
                """;

        int affected = jdbcTemplate.update(
                sql,
                snapshotTime,
                snapshotTime,
                lookbackMinutes,
                snapshotTime,
                snapshotTime,
                lookbackMinutes,
                Math.max(0, minUpdateGapSeconds)
        );
        log.info("reuse_pattern refreshed incrementally, lookbackMinutes={}, snapshotTime={}, minGapSeconds={}, affectedRows={}",
                lookbackMinutes, snapshotTime, minUpdateGapSeconds, affected);
        return new RefreshSummary(affected, snapshotTime.toString(), lookbackMinutes, effectiveMinGapSeconds, scanUpdateLagSeconds);
    }

    public record RefreshSummary(int affectedRows,
                                 String snapshotTime,
                                 int lookbackMinutes,
                                 int minUpdateGapSeconds,
                                 int scanUpdateLagSeconds) {
    }
}
