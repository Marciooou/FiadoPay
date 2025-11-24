# FiadoPay Simulator (Spring Boot + H2)

Gateway de pagamento FiadoPay, utilizado na disciplina de AVI/POOA, refatorado com foco em engenharia, anotações customizadas, reflexão, plugins, processamento assíncrono, boa arquitetura e manutenção do contrato da API original.
Substitui PSPs reais com um backend simples em memória (H2).

## 🚀 Como Rodar
```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```
- H2 console: http://localhost:8080/h2
- JDBC: jdbc:h2:mem:fiadopay
- Swagger UI: http://localhost:8080/swagger-ui.html

## 👥 Integrantes da Equipe ✍️
- [Márcio Augusto Silva Ramos Filho]
- [Guilherme da Vitoria Souza da Silva]
- [Carlos Murilo dos Santos Nery]
- [Davi Moreira Lima]
- [Nilo Calvin Vieira Dos Santos]

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

## 🏷️ 3. Anotações Criadas (e Metadados)
🔹 @PaymentMethod
Define um método de pagamento (CARD, PIX, BOLETO, DEBIT).
Campos:
- type
- supportsInstallments

🔹 @AntiFraud
Regra antifraude plugável.
Campos:
- name
- threshold
- order

🔹 @WebhookSink
Para identificar handlers de envio de webhook.
Campos:
- eventType

## 🔍 4. Mecanismo de Reflexão
O Spring escaneia o classpath e registra automaticamente:
- Handlers de pagamento
- Regras antifraude
- Sinks de webhook

Exemplo:
var beans = context.getBeansWithAnnotation(PaymentMethod.class);
Isso permite adicionar novos comportamentos sem alterar o PaymentService.

## ⚙️ 5. Threads (ExecutorService)
O processamento é assíncrono e concorrente:
executor.submit(() -> processPayment(paymentId));
Benefícios:
- Pagamentos não travam requisições
- Webhooks enviados em paralelo
- Mantém a API responsiva

## 🧱 6. Padrões Aplicados
<table> <thead> <tr> <th style="text-align:left;">Padrão</th> <th style="text-align:left;">Onde foi aplicado</th> </tr> </thead> <tbody> <tr> <td><strong>Strategy</strong></td> <td>Handlers de pagamento, regras antifraude e sinks de webhook</td> </tr> <tr> <td><strong>Plugin Architecture</strong></td> <td>Uso de anotações + reflexão para carregar dinamicamente módulos</td> </tr> <tr> <td><strong>SRP (Single Responsibility Principle)</strong></td> <td>Serviços isolados para cada responsabilidade core</td> </tr> <tr> <td><strong>Factory via Reflexão</strong></td> <td>Registries localizam e instanciam plugins no startup</td> </tr> <tr> <td><strong>Template Method</strong></td> <td>Execução ordenada das regras antifraude</td> </tr> </tbody> </table>

## ⚠️ 7. Limites Conhecidos
- Banco H2 em memória (não persiste após restart)
- Webhooks não fazem retry após reiniciar o app
- Autenticação é fake (exigência do contrato)
- Sem fila externa (Kafka/Rabbit)
- AntiFraud simples (limite numérico)

## 📸 8. Evidências (Prints)
Recomendação: coloque os arquivos em:
docs/prints/

Prints esperados:
1. Aplicação rodando + Swagger
2. Merchant criado
3. Token gerado
4. Pagamento criado
5. Pagamento aprovado/recusado (assíncrono)
6. H2 Console mostrando tabelas
7. WEBHOOK_DELIVERY com assinatura
8. AntiFraud aplicando recusa

Prints: (https://docs.google.com/document/d/1Xe_ZLAoDAWCvvPXVkDJlhoo15_gaqNaz4i-PS3UdK2s/edit?usp=sharing)**


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
