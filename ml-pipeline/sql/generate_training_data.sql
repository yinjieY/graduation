-- Generate synthetic scan training data for offline risk-model iteration.
-- Target table: yx_scan_anomaly.scan_log

USE yx_scan_anomaly;

DROP PROCEDURE IF EXISTS gen_scan_training_data;
DELIMITER $$
CREATE PROCEDURE gen_scan_training_data(IN total_rows INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < total_rows DO
        INSERT INTO scan_log(
            qs_id, batch_id, company_id, scan_time,
            ip, ip_masked, device_fingerprint, browser,
            lat, lng, location_source, distance_km,
            is_first, new_device, risk_device, created_at
        ) VALUES (
            CONCAT('QS', LPAD(FLOOR(RAND() * 2000), 8, '0')),
            CONCAT('B', LPAD(FLOOR(RAND() * 600), 6, '0')),
            CONCAT('C', LPAD(FLOOR(RAND() * 300), 6, '0')),
            DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30 * 24 * 60) MINUTE),
            CONCAT(FLOOR(RAND()*255), '.', FLOOR(RAND()*255), '.', FLOOR(RAND()*255), '.', FLOOR(RAND()*255)),
            CONCAT(FLOOR(RAND()*255), '.', FLOOR(RAND()*255), '.', FLOOR(RAND()*255), '.*'),
            CONCAT('dev-', LPAD(FLOOR(RAND()*5000), 5, '0')),
            IF(RAND() < 0.5, 'WeChat', 'Browser'),
            27 + RAND() * 4,
            110 + RAND() * 6,
            IF(RAND() < 0.65, 'gps', 'ip'),
            RAND() * 150,
            IF(RAND() < 0.08, 1, 0),
            IF(RAND() < 0.3, 1, 0),
            IF(RAND() < 0.06, 1, 0),
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END $$
DELIMITER ;

-- Example:
-- CALL gen_scan_training_data(50000);

