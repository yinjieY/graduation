package org.hunau.alert.service;

import jakarta.annotation.PostConstruct;
import org.hunau.alert.model.AlertRule;
import org.hunau.alert.rule.AlertRiskFact;
import org.hunau.alert.rule.RuleEngineResult;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AlertRuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleEngineService.class);
    private static final Pattern THRESHOLD_PATTERN = Pattern.compile(".*>=\\s*(\\d+).*");

    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<KieBase> kieBaseRef = new AtomicReference<>();
    private final AtomicReference<List<AlertRule>> activeRulesRef = new AtomicReference<>(List.of());
    private final AtomicLong loadedAtEpochMs = new AtomicLong(0L);
    private volatile LocalDateTime latestRuleUpdateTime;

    public AlertRuleEngineService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        reloadNow();
    }

    @Scheduled(fixedDelayString = "${app.rules.refresh-ms:10000}",
            initialDelayString = "${app.rules.initial-delay-ms:3000}")
    public void refreshIfChanged() {
        LocalDateTime dbLatest = jdbcTemplate.query(
                "SELECT MAX(update_time) AS max_time FROM alert_rule",
                rs -> rs.next() ? toLocalDateTime(rs, "max_time") : null
        );
        if (dbLatest == null) {
            return;
        }
        LocalDateTime localLatest = latestRuleUpdateTime;
        if (localLatest == null || dbLatest.isAfter(localLatest)) {
            reloadNow();
        }
    }

    public synchronized Map<String, Object> reloadNow() {
        List<AlertRule> activeRules = loadActiveRules();
        KieBase kieBase = buildKieBase(activeRules);
        kieBaseRef.set(kieBase);
        activeRulesRef.set(activeRules);
        loadedAtEpochMs.set(System.currentTimeMillis());
        latestRuleUpdateTime = activeRules.stream()
                .map(AlertRule::getUpdateTime)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        log.info("drools rules reloaded, activeRules={}", activeRules.size());
        return Map.of(
                "activeRuleCount", activeRules.size(),
                "loadedAt", loadedAtEpochMs.get(),
                "latestRuleUpdateTime", String.valueOf(latestRuleUpdateTime)
        );
    }

    public RuleEngineResult evaluate(int scanCount1h, int deviceCount1d, int ipCount1h) {
        KieBase kieBase = kieBaseRef.get();
        if (kieBase == null) {
            reloadNow();
            kieBase = kieBaseRef.get();
        }

        AlertRiskFact fact = new AlertRiskFact(scanCount1h, deviceCount1d, ipCount1h);
        if (kieBase == null) {
            return new RuleEngineResult(0.0, fact.getHitRuleIds(), fact.getHitReasons(), 0, loadedAtEpochMs.get());
        }

        KieSession session = null;
        try {
            session = kieBase.newKieSession();
            session.insert(fact);
            session.fireAllRules();
        } finally {
            if (session != null) {
                session.dispose();
            }
        }
        return new RuleEngineResult(
                fact.getRuleScore(),
                fact.getHitRuleIds(),
                fact.getHitReasons(),
                activeRulesRef.get().size(),
                loadedAtEpochMs.get()
        );
    }

    public List<AlertRule> listRules() {
        return jdbcTemplate.query(
                "SELECT rule_id,rule_name,rule_content,threshold,score_weight,alert_level,status,update_time FROM alert_rule ORDER BY rule_id",
                this::mapRule
        );
    }

    public void updateThreshold(String ruleId, String threshold) {
        jdbcTemplate.update("UPDATE alert_rule SET threshold=?, update_time=NOW() WHERE rule_id=?", threshold, ruleId);
    }

    public void updateScoreWeight(String ruleId, Double scoreWeight) {
        jdbcTemplate.update("UPDATE alert_rule SET score_weight=?, update_time=NOW() WHERE rule_id=?", scoreWeight, ruleId);
    }

    public void updateStatus(String ruleId, Integer status) {
        jdbcTemplate.update("UPDATE alert_rule SET status=?, update_time=NOW() WHERE rule_id=?", status, ruleId);
    }

    private KieBase buildKieBase(List<AlertRule> activeRules) {
        if (activeRules.isEmpty()) {
            return null;
        }

        StringBuilder drl = new StringBuilder();
        drl.append("package org.hunau.alert.rules;\n")
                .append("import org.hunau.alert.rule.AlertRiskFact;\n");

        for (AlertRule rule : activeRules) {
            MetricMapping mapping = resolveMetric(rule);
            if (mapping == null) {
                continue;
            }
            int threshold = parseThreshold(rule.getThreshold(), mapping.defaultThreshold());
            double scoreWeight = mapping.scoreWeight();
            String escapedName = escapeDrlText(rule.getRuleName());
            String escapedRuleId = escapeDrlText(rule.getRuleId());
            String reason = escapedName + "(" + mapping.displayMetric() + ">=" + threshold + ")";

            drl.append("rule \"").append(escapedRuleId).append("\"\n")
                    .append("when\n")
                    .append("    $f : AlertRiskFact()\n")
                    .append("    eval(").append(mapping.getterExpr()).append(" >= ").append(threshold).append(")\n")
                    .append("then\n")
                    .append("    $f.addHit(\"").append(escapedRuleId).append("\", \"").append(escapeDrlText(reason)).append("\");\n")
                    .append("    $f.increaseRuleScore(").append(scoreWeight).append("d);\n")
                    .append("end\n\n");
        }

        KieHelper helper = new KieHelper();
        helper.addContent(drl.toString(), ResourceType.DRL);
        return helper.build();
    }

    private List<AlertRule> loadActiveRules() {
        return jdbcTemplate.query(
                "SELECT rule_id,rule_name,rule_content,threshold,score_weight,alert_level,status,update_time FROM alert_rule WHERE status=1 ORDER BY rule_id",
                this::mapRule
        );
    }

    private AlertRule mapRule(ResultSet rs, int rowNum) throws SQLException {
        AlertRule rule = new AlertRule();
        rule.setRuleId(rs.getString("rule_id"));
        rule.setRuleName(rs.getString("rule_name"));
        rule.setRuleContent(rs.getString("rule_content"));
        rule.setThreshold(rs.getString("threshold"));
        rule.setScoreWeight(rs.getDouble("score_weight"));
        rule.setAlertLevel(rs.getInt("alert_level"));
        rule.setStatus(rs.getInt("status"));
        rule.setUpdateTime(toLocalDateTime(rs, "update_time"));
        return rule;
    }

    private MetricMapping resolveMetric(AlertRule rule) {
        String content = rule.getRuleContent() == null ? "" : rule.getRuleContent().toLowerCase(Locale.ROOT);
        String ruleName = rule.getRuleName() == null ? "" : rule.getRuleName().toLowerCase(Locale.ROOT);
        String ruleId = rule.getRuleId() == null ? "" : rule.getRuleId().toLowerCase(Locale.ROOT);
        
        double dbWeight = rule.getScoreWeight() != null ? rule.getScoreWeight() : 0.0;

        if (content.contains("scan_count_1h") || content.contains("scans_1h") || ruleId.contains("scan") || ruleName.contains("高频")) {
            return new MetricMapping("$f.getScanCount1h()", "1h扫码次数", 5, dbWeight > 0 ? dbWeight : 0.50);
        }
        if (content.contains("device_count_1d") || content.contains("device_1d") || ruleId.contains("device") || ruleName.contains("设备")) {
            return new MetricMapping("$f.getDeviceCount1d()", "1d设备数", 10, dbWeight > 0 ? dbWeight : 0.40);
        }
        if (content.contains("ip_count_1h") || content.contains("ip_1h") || ruleId.contains("ip") || ruleName.contains("ip")) {
            return new MetricMapping("$f.getIpCount1h()", "1hIP数", 20, dbWeight > 0 ? dbWeight : 0.45);
        }
        return null;
    }

    private int parseThreshold(String thresholdText, int defaultValue) {
        if (thresholdText == null || thresholdText.isBlank()) {
            return defaultValue;
        }
        Matcher matcher = THRESHOLD_PATTERN.matcher(thresholdText.replace(" ", ""));
        if (!matcher.matches()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private String escapeDrlText(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record MetricMapping(String getterExpr,
                                 String displayMetric,
                                 int defaultThreshold,
                                 double scoreWeight) {
    }
}



