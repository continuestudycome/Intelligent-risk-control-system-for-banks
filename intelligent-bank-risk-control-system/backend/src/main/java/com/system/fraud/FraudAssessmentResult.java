package com.system.fraud;

import java.math.BigDecimal;
import java.util.List;

public record FraudAssessmentResult(
        FraudRiskLevel level,
        BigDecimal mlScore,
        String mlModelVersion,
        List<String> hitRuleCodes,
        String featureSnapshotJson
) {
}
