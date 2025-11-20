# FiadoPay Simulator (Spring Boot + H2)

Gateway de pagamento FiadoPay, utilizado na disciplina de AVI/POOA, refatorado com foco em engenharia, anotações customizadas, reflexão, plugins, processamento assíncrono, boa arquitetura e manutenção do contrato da API original.
Substitui PSPs reais com um backend simples em memória (H2).

## 🚀 Como Rodar
```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

H2 console: http://localhost:8080/h2  
JDBC: jdbc:h2:mem:fiadopay
Swagger UI: http://localhost:8080/swagger-ui.html


## 🧩 1. Contexto Escolhido
Opção 1: Este FiadoPay funciona como um PSP simulado, permitindo que lojas criem pagamentos, acompanhem seu status e recebam webhooks, mas agora com uma arquitetura flexível e extensível via plugins.
A refatoração exigida pelo professor inclui:
- Anotações para métodos de pagamento, regras antifraude e webhooks
- Descoberta dinâmica por reflexão
- Execução assíncrona
- Manutenção das rotas originais
- Sem dependência da IDE

## 🧠 2. Decisões de Design (Arquitetura)

✔ Arquitetura por Plugins (Strategy + Reflection)
Criamos handlers para métodos de pagamento com anotação:
@PaymentMethod(type="CARD", supportsInstallments=true)

✔ AntiFraude Modular
Regras isoladas, anotadas e descobertas automaticamente:
@AntiFraud(name="HighAmount", threshold=5000)

✔ Webhooks desacoplados
Enviadores de webhook são plugins com:
@WebhookSink(eventType="payment.updated")

✔ Processamento Assíncrono Real
Substituímos Thread.sleep() por:
ExecutorService executor = Executors.newFixedThreadPool(10);

✔ SRP + Clean Architecture

- PaymentService = orquestração
- Handlers = lógica isolada
- Registry = descoberta automática
- WebhookDispatcher = lado externo


## 🔄 9. Fluxo

1) **Cadastrar merchant**
```bash
curl -X POST http://localhost:8080/fiadopay/admin/merchants   -H "Content-Type: application/json"   -d '{"name":"MinhaLoja ADS","webhookUrl":"http://localhost:8081/webhooks/payments"}'
```

2) **Obter token**
```bash
curl -X POST http://localhost:8080/fiadopay/auth/token   -H "Content-Type: application/json"   -d '{"client_id":"<clientId>","client_secret":"<clientSecret>"}'
```

3) **Criar pagamento**
```bash
curl -X POST http://localhost:8080/fiadopay/gateway/payments   -H "Authorization: Bearer FAKE-<merchantId>"   -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000"   -H "Content-Type: application/json"   -d '{"method":"CARD","currency":"BRL","amount":250.50,"installments":12,"metadataOrderId":"ORD-123"}'
```

4) **Consultar pagamento**
```bash
curl http://localhost:8080/fiadopay/gateway/payments/<paymentId>
```
