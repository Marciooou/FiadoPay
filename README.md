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
