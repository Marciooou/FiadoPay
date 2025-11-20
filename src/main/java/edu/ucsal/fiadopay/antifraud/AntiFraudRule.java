package edu.ucsal.fiadopay.antifraud;

import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.Merchant;

public interface AntiFraudRule {

    void validate(Payment payment, Merchant merchant);
}