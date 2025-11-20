package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.antifraud.AntiFraudRegistry;
import edu.ucsal.fiadopay.controller.PaymentRequest;
import edu.ucsal.fiadopay.controller.PaymentResponse;
import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.payment.PaymentMethodRegistry;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.PaymentRepository;
import edu.ucsal.fiadopay.webhook.WebhookDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class PaymentService {

    private final MerchantRepository merchants;
    private final PaymentRepository payments;

    private final PaymentMethodRegistry paymentRegistry;
    private final AntiFraudRegistry antiFraudRegistry;
    private final WebhookDispatcher webhookDispatcher;
    private final ExecutorService executor;

    @Value("${fiadopay.processing-delay-ms}")
    long delay;

    @Value("${fiadopay.failure-rate}")
    double failRate;

    public PaymentService(MerchantRepository merchants,
                          PaymentRepository payments,
                          PaymentMethodRegistry paymentRegistry,
                          AntiFraudRegistry antiFraudRegistry,
                          WebhookDispatcher webhookDispatcher,
                          ExecutorService fiadopayExecutorService) {

        this.merchants = merchants;
        this.payments = payments;
        this.paymentRegistry = paymentRegistry;
        this.antiFraudRegistry = antiFraudRegistry;
        this.webhookDispatcher = webhookDispatcher;
        this.executor = fiadopayExecutorService;
    }

    /**
     * Extrai o merchant a partir do token "Bearer FAKE-{id}" e garante que está ACTIVE.
     */
    private Merchant merchantFromAuth(String auth) {
        if (auth == null || !auth.startsWith("Bearer FAKE-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String raw = auth.substring("Bearer FAKE-".length());
        long id;
        try {
            id = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Merchant merchant = merchants.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (merchant.getStatus() != Merchant.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return merchant;
    }

    /**
     * Cria um pagamento com idempotência, aplicação de regras de método de pagamento
     * (@PaymentMethod) e antifraude (@AntiFraud). O processamento final e webhook é assíncrono.
     */
    @Transactional
    public PaymentResponse createPayment(String auth, String idemKey, PaymentRequest req) {
        Merchant merchant = merchantFromAuth(auth);
        Long mid = merchant.getId();

        // Idempotência: se já existe pagamento com essa chave para esse merchant, retorna o mesmo
        if (idemKey != null) {
            var existing = payments.findByIdempotencyKeyAndMerchantId(idemKey, mid);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        // Monta o pagamento base
        Payment payment = Payment.builder()
                .id("pay_" + UUID.randomUUID().toString().substring(0, 8))
                .merchantId(mid)
                .method(req.method().toUpperCase())
                .amount(req.amount())
                .currency(req.currency())
                .installments(req.installments() == null ? 1 : req.installments())
                // monthlyInterest e totalWithInterest serão ajustados pelo handler
                .status(Payment.Status.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .idempotencyKey(idemKey)
                .metadataOrderId(req.metadataOrderId())
                .build();

        // 1) Regras específicas do método de pagamento (juros, parcelas, etc.)
        paymentRegistry
                .getHandler(req.method())
                .applyBusinessRules(payment, req, merchant);

        // 2) Regras de antifraude (podem reprovar o pagamento via exceção)
        antiFraudRegistry.getRules()
                .forEach(rule -> rule.validate(payment, merchant));

        // 3) Persiste o pagamento PENDING
        payments.save(payment);

        // 4) Processamento assíncrono (aprovação / recusa + webhook)
        executor.submit(() -> processAndNotify(payment.getId(), merchant.getId()));

        return toResponse(payment);
    }

    public PaymentResponse getPayment(String id) {
        Payment p = payments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toResponse(p);
    }

    /**
     * Estorno: só permite refund de pagamentos APPROVED e pertencentes ao merchant autenticado.
     * Dispara webhook de atualização de pagamento.
     */
    @Transactional
    public Map<String, Object> refund(String auth, String paymentId) {
        Merchant merchant = merchantFromAuth(auth);

        Payment p = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!merchant.getId().equals(p.getMerchantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (p.getStatus() != Payment.Status.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approved payments can be refunded");
        }

        p.setStatus(Payment.Status.REFUNDED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        // dispara evento payment.updated para os sinks anotados com @WebhookSink
        webhookDispatcher.dispatchPaymentUpdated(p, merchant);

        // resposta simples, mantendo ideia de "ref_xxx"
        return Map.of(
                "id", "ref_" + UUID.randomUUID().toString().substring(0, 8),
                "status", p.getStatus().name()
        );
    }

    /**
     * Simula o tempo de processamento, aprova/recusa baseado em failRate e envia webhook.
     */
    private void processAndNotify(String paymentId, Long merchantId) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return;
        }

        Payment p = payments.findById(paymentId).orElse(null);
        Merchant m = merchants.findById(merchantId).orElse(null);
        if (p == null || m == null) {
            return;
        }

        boolean approved = Math.random() > failRate;
        p.setStatus(approved ? Payment.Status.APPROVED : Payment.Status.DECLINED);
        p.setUpdatedAt(Instant.now());
        payments.save(p);

        // Notifica todos os sinks de webhook (ex.: envio HTTP + persist de WebhookDelivery)
        webhookDispatcher.dispatchPaymentUpdated(p, m);
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getStatus().name(),
                p.getMethod(),
                p.getAmount(),
                p.getInstallments(),
                p.getMonthlyInterest(),
                p.getTotalWithInterest()
        );
        }
}
