package org.hunau.alert.service;

import org.hunau.common.enums.RiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertTriggerStatusServiceTest {

    @Autowired
    private AlertTriggerStatusService alertTriggerStatusService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String QS_ID = "TEST_QR_001";
    private static final String COMPANY_ID = "TEST_COMPANY_001";
    private static final String RULE_ID = "R001";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM alert_action");
        jdbcTemplate.execute("DELETE FROM alert_record");
        jdbcTemplate.execute("DELETE FROM alert_trigger_status");
    }

    @Test
    @DisplayName("首次触发警告规则时，应返回未触发状态")
    void testFirstTriggerShouldReturnNotTriggered() {
        AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 1L);

        assertFalse(result.alreadyTriggered(), "首次触发时 alreadyTriggered 应为 false");
        assertEquals(1, result.triggerCount(), "首次触发时触发次数应为 1");
        assertNull(result.previousAlertId(), "首次触发时 previousAlertId 应为 null");
    }

    @Test
    @DisplayName("重复触发相同警告规则时，应返回已触发状态")
    void testRepeatTriggerShouldReturnAlreadyTriggered() {
        alertTriggerStatusService.checkAndUpdateTriggerStatus(QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 1L);
        
        AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 2L);

        assertTrue(result.alreadyTriggered(), "重复触发时 alreadyTriggered 应为 true");
        assertEquals(2, result.triggerCount(), "重复触发时触发次数应为 2");
        assertEquals(1L, result.previousAlertId(), "重复触发时应返回之前的 alertId");
    }

    @Test
    @DisplayName("不同规则触发时，应视为首次触发")
    void testDifferentRuleShouldBeTreatedAsFirstTrigger() {
        alertTriggerStatusService.checkAndUpdateTriggerStatus(QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 1L);
        
        AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                QS_ID, COMPANY_ID, "R002", RiskLevel.MEDIUM, 2L);

        assertFalse(result.alreadyTriggered(), "不同规则触发时应为首次触发");
        assertEquals(1, result.triggerCount(), "不同规则触发时触发次数应为 1");
    }

    @Test
    @DisplayName("不同二维码触发相同规则时，应视为首次触发")
    void testDifferentQsIdShouldBeTreatedAsFirstTrigger() {
        alertTriggerStatusService.checkAndUpdateTriggerStatus(QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 1L);
        
        AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                "TEST_QR_002", COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 2L);

        assertFalse(result.alreadyTriggered(), "不同二维码触发相同规则时应为首次触发");
        assertEquals(1, result.triggerCount(), "不同二维码触发时触发次数应为 1");
    }

    @Test
    @DisplayName("状态解析后应能重新触发")
    void testResolveStatusAllowsNewTrigger() {
        alertTriggerStatusService.checkAndUpdateTriggerStatus(QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 1L);
        alertTriggerStatusService.resolveStatus(QS_ID, RULE_ID);
        
        AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, 2L);

        assertFalse(result.alreadyTriggered(), "已解析的状态应允许重新触发");
        assertEquals(1, result.triggerCount(), "重新触发时触发次数应为 1");
    }

    @Test
    @DisplayName("多次重复触发应正确递增触发次数")
    void testMultipleTriggersShouldIncrementCount() {
        int triggerCount = 5;
        Long lastAlertId = null;
        
        for (int i = 1; i <= triggerCount; i++) {
            AlertTriggerStatusService.TriggerStatusResult result = alertTriggerStatusService.checkAndUpdateTriggerStatus(
                    QS_ID, COMPANY_ID, RULE_ID, RiskLevel.MEDIUM, (long) i);
            
            assertEquals(i == 1 ? false : true, result.alreadyTriggered());
            assertEquals(i, result.triggerCount());
            assertEquals(lastAlertId, result.previousAlertId());
            lastAlertId = (long) i;
        }
    }

    @Test
    @DisplayName("获取状态应返回正确的记录")
    void testGetStatusShouldReturnCorrectRecord() {
        alertTriggerStatusService.checkAndUpdateTriggerStatus(QS_ID, COMPANY_ID, RULE_ID, RiskLevel.HIGH, 1L);
        
        var statusOpt = alertTriggerStatusService.getStatus(QS_ID, RULE_ID);
        
        assertTrue(statusOpt.isPresent(), "应能获取到状态记录");
        assertEquals(QS_ID, statusOpt.get().getQsId());
        assertEquals(COMPANY_ID, statusOpt.get().getCompanyId());
        assertEquals(RULE_ID, statusOpt.get().getRuleId());
        assertEquals("HIGH", statusOpt.get().getRiskLevel());
        assertEquals(1, statusOpt.get().getTriggerCount());
        assertEquals("ACTIVE", statusOpt.get().getStatus());
    }

    @Test
    @DisplayName("获取不存在的状态应返回空")
    void testGetStatusShouldReturnEmptyForNonExistent() {
        var statusOpt = alertTriggerStatusService.getStatus("NON_EXISTENT", RULE_ID);
        
        assertFalse(statusOpt.isPresent(), "不存在的状态应返回空");
    }
}