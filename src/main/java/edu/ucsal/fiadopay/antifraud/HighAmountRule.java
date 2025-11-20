package edu.ucsal.fiadopay.antifraud;

import edu.ucsal.fiadopay.annotation.AntiFraud;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@AntiFraud(name = "HighAmount", threshold = 5000.00, order = 1)
public class HighAmountRule implements AntiFraudRule {

    @Override
    public void validate(Payment payment, Merchant merchant) {
        AntiFraud ann = this.getClass().getAnnotation(AntiFraud.class);
        BigDecimal threshold = BigDecimal.valueOf(ann.threshold());

        if (payment.getAmount().compareTo(threshold) > 0) {
           
            throw new IllegalStateException("Pagamento reprovado por valor alto (regra HighAmount)");
        }
    }
}