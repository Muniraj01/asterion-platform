package com.asterion.payment.application.port.out;

import com.asterion.payment.application.model.RiskAssessmentResult;
import com.asterion.payment.domain.model.Payment;

public interface RiskAssessmentPort {

    RiskAssessmentResult assess(Payment payment);
}