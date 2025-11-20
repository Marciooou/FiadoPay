package edu.ucsal.fiadopay.payment;

import edu.ucsal.fiadopay.annotation.PaymentMethod;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@PaymentMethod(type = "CARD", supportsInstallments = true)
public class CardPaymentHandler implements PaymentMethodHandler {

    @Override
    public void applyBusinessRules(Payment payment, PaymentRequest request, Merchant merchant) {
        Integer installments = request.installments() == null ? 1 : request.installments();
        payment.setInstallments(installments);

        double monthlyInterest = 0.0;
        BigDecimal total = request.amount();

        if (installments > 1) {
            monthlyInterest = 1.0; // 1% a.m
            BigDecimal base = new BigDecimal("1.01");
            BigDecimal factor = base.pow(installments);
            total = request.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        payment.setMonthlyInterest(monthlyInterest);
        payment.setTotalWithInterest(total);
    }
}