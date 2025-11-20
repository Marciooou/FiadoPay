package edu.ucsal.fiadopay.payment;

import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.Merchant;

public interface PaymentMethodHandler {

    void applyBusinessRules(Payment payment, PaymentRequest request, Merchant merchant);
}