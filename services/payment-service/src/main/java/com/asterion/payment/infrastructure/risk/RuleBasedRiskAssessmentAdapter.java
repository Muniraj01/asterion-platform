package com.asterion.payment.infrastructure.risk;

import com.asterion.payment.application.model.RiskAssessmentResult;
import com.asterion.payment.application.port.out.RiskAssessmentPort;
import com.asterion.payment.domain.model.Payment;
import com.asterion.payment.domain.model.RiskDecision;

import java.math.BigDecimal;
import java.util.Objects;

public final class RuleBasedRiskAssessmentAdapter implements RiskAssessmentPort {

    private final BigDecimal reviewAboveAmount;
    private final BigDecimal rejectAboveAmount;

    public RuleBasedRiskAssessmentAdapter(
            BigDecimal reviewAboveAmount, BigDecimal rejectAboveAmount) {

        this.reviewAboveAmount = Objects.requireNonNull(
                reviewAboveAmount, "Review threshold must not be null");

        if (reviewAboveAmount.signum() < 0)
            throw new IllegalArgumentException("Review threshold must not be negative");

        if (rejectAboveAmount != null
                && rejectAboveAmount.compareTo(reviewAboveAmount) <= 0)
            throw new IllegalArgumentException(
                    "Rejection threshold must be greater than review threshold");

        if (rejectAboveAmount != null && rejectAboveAmount.signum() < 0)
            throw new IllegalArgumentException("Rejection threshold must not be negative");

        this.rejectAboveAmount = rejectAboveAmount;
    }

    @Override
    public RiskAssessmentResult assess(Payment payment) {
        BigDecimal amount = payment.totalAmount();
        if (rejectAboveAmount != null && amount.compareTo(rejectAboveAmount) >= 0) {
            return new RiskAssessmentResult(RiskDecision.REJECT,
                    "AMOUNT_EXCEEDS_REJECTION_THRESHOLD");
        }

        if (amount.compareTo(reviewAboveAmount) > 0) {
            return new RiskAssessmentResult(RiskDecision.REVIEW,
                    "AMOUNT_REQUIRES_MANUAL_REVIEW");
        }

        return new RiskAssessmentResult(RiskDecision.APPROVE,
                "AMOUNT_WITHIN_AUTO_APPROVAL_THRESHOLD");
    }
}