package com.asterion.payment.infrastructure.risk;

import com.asterion.payment.application.model.RiskAssessmentResult;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.RiskDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleBasedRiskAssessmentAdapterTest {

    private Payment payment(String amount) {
        Instant now = Instant.parse("2026-10-27T10:00:00Z");

        return Payment.initiate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal(amount),
                now);
    }

    @Test
    void shouldApproveAmountWithinReviewThreshold() {
        var adapter = new RuleBasedRiskAssessmentAdapter(
                new BigDecimal("10000.00"), null);

        RiskAssessmentResult result = adapter.assess(payment("10000.00"));

        assertEquals(RiskDecision.APPROVE, result.decision());
        assertEquals("AMOUNT_WITHIN_AUTO_APPROVAL_THRESHOLD",
                result.reasonCode());
    }

    @Test
    void shouldHoldAmountAboveReviewThreshold() {
        var adapter = new RuleBasedRiskAssessmentAdapter(
                new BigDecimal("10000.00"), null);

        RiskAssessmentResult result = adapter.assess(payment("10000.01"));

        assertEquals(RiskDecision.REVIEW, result.decision());
        assertEquals("AMOUNT_REQUIRES_MANUAL_REVIEW", result.reasonCode());
    }

    @Test
    void shouldRejectAmountAtConfiguredRejectionThreshold() {
        var adapter = new RuleBasedRiskAssessmentAdapter(
                new BigDecimal("10000.00"),
                new BigDecimal("50000.00"));

        RiskAssessmentResult result = adapter.assess(payment("50000.00"));

        assertEquals(RiskDecision.REJECT, result.decision());
        assertEquals("AMOUNT_EXCEEDS_REJECTION_THRESHOLD",
                result.reasonCode());
    }

    @Test
    void shouldRequireRejectionThresholdToExceedReviewThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> new RuleBasedRiskAssessmentAdapter(
                        new BigDecimal("10000.00"),
                        new BigDecimal("10000.00")));
    }
}