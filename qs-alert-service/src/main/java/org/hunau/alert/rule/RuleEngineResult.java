package org.hunau.alert.rule;

import java.util.Collections;
import java.util.List;

public record RuleEngineResult(double ruleScore,
                               List<String> hitRuleIds,
                               List<String> hitReasons,
                               int activeRuleCount,
                               long loadedAtEpochMs) {

    public RuleEngineResult {
        hitRuleIds = hitRuleIds == null ? Collections.emptyList() : List.copyOf(hitRuleIds);
        hitReasons = hitReasons == null ? Collections.emptyList() : List.copyOf(hitReasons);
    }
}

