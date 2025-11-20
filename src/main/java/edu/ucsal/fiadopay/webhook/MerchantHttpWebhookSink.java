package edu.ucsal.fiadopay.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.annotation.WebhookSink;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
@WebhookSink(eventType = "payment.updated")
public class MerchantHttpWebhookSink implements WebhookSinkHandler {

    private final WebhookDeliveryRepository deliveries;
    private final ObjectMapper objectMapper;

    @Value("${fiadopay.webhook-secret}")
    private String secret;

    public MerchantHttpWebhookSink(WebhookDeliveryRepository deliveries, ObjectMapper objectMapper) {
        this.deliveries = deliveries;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendPaymentUpdated(Payment payment, Merchant merchant) throws Exception {
        if (merchant.getWebhookUrl() == null || merchant.getWebhookUrl().isBlank()) {
            return;
        }

        String eventId = "evt_" + payment.getId();
        Map<String, Object> payloadMap = Map.of(
                "id", eventId,
                "type", "payment.updated",
                "data", Map.of(
                        "object", Map.of(
                                "id", payment.getId(),
                                "status", payment.getStatus().name(),
                                "amount", payment.getAmount(),
                                "method", payment.getMethod()
                        )
                )
        );

        String payload = objectMapper.writeValueAsString(payloadMap);
        String signature = sign(payload);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(merchant.getWebhookUrl()))
                .header("Content-Type", "application/json")
                .header("X-FiadoPay-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;

        deliveries.save(WebhookDelivery.builder()
                .eventId(eventId)
                .eventType("payment.updated")
                .paymentId(payment.getId())
                .targetUrl(merchant.getWebhookUrl())
                .signature(signature)
                .payload(payload)
                .attempts(1)
                .delivered(ok)
                .lastAttemptAt(Instant.now())
                .build());
    }

    private String sign(String payload) throws Exception {
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
    }
}