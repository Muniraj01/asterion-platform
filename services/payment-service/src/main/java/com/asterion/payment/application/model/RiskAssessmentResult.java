package com.asterion.payment.application.model;

import com.asterion.payment.domain.model.RiskDecision;

import java.util.Objects;

public record RiskAssessmentResult(RiskDecision decision, String reasonCode) {

    public RiskAssessmentResult {
        Objects.requireNonNull(decision, "Risk decision must not be null");

        if (reasonCode == null || reasonCode.isBlank())
            throw new IllegalArgumentException("Risk reason code must not be blank");

        reasonCode = reasonCode.trim();
    }
}