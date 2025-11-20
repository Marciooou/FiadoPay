package edu.ucsal.fiadopay.webhook;

import edu.ucsal.fiadopay.annotation.WebhookSink;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WebhookDispatcher {

    private final List<WebhookSinkHandler> sinks;

    public WebhookDispatcher(ApplicationContext context) {
        Map<String, Object> beans = context.getBeansWithAnnotation(WebhookSink.class);
        this.sinks = beans.values().stream()
                .map(b -> (WebhookSinkHandler) b)
                .toList();
    }

    public void dispatchPaymentUpdated(Payment payment, Merchant merchant) {
        for (WebhookSinkHandler sink : sinks) {
            try {
                sink.sendPaymentUpdated(payment, merchant);
            } catch (Exception e) {
           
            }
        }
    }
}