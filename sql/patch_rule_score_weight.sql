ALTER TABLE alert_rule ADD COLUMN score_weight DOUBLE DEFAULT 0.5 COMMENT '规则分数权重' AFTER threshold;

UPDATE alert_rule SET score_weight = 0.50 WHERE rule_id = 'R001';
UPDATE alert_rule SET score_weight = 0.40 WHERE rule_id = 'R002';
UPDATE alert_rule SET score_weight = 0.45 WHERE rule_id = 'R003';

COMMIT;
