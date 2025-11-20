package edu.ucsal.fiadopay.webhook;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;

public interface WebhookSinkHandler {

    void sendPaymentUpdated(Payment payment, Merchant merchant) throws Exception;
}
