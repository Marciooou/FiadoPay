package edu.ucsal.fiadopay.payment;

import edu.ucsal.fiadopay.annotation.PaymentMethod;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;

import org.springframework.stereotype.Component;

@Component
@PaymentMethod(type = "DEBIT", supportsInstallments = false)
public class DebitPaymentHandler implements PaymentMethodHandler {

    @Override
    public void applyBusinessRules(Payment payment, PaymentRequest request, Merchant merchant) {
        payment.setInstallments(1);
        payment.setMonthlyInterest(0.0);
        payment.setTotalWithInterest(request.amount());
        
    }
}