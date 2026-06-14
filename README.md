# 🍔 Lanchonete API

API RESTful para gerenciamento de pedidos de lanchonete, com arquitetura orientada a eventos usando **RabbitMQ**.

## 🏗️ Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.2.x | Framework base |
| Spring Data JPA | 3.2.x | Persistência |
| Spring AMQP | 3.2.x | Mensageria com RabbitMQ |
| H2 Database | — | Banco em memória (dev/test) |
| Lombok | — | Redução de boilerplate |
| RabbitMQ | 3.13 | Message broker |

---

## 🚀 Como executar

### Pré-requisitos
- Java 21+
- Maven 3.8+
- Docker + Docker Compose

### 1. Subir o RabbitMQ (infraestrutura)
```bash
docker-compose up -d
```

### 2. Iniciar a aplicação
```bash
./mvnw spring-boot:run
```

### 3. Acessar as interfaces
| Interface | URL | Credenciais |
|---|---|---|
| API REST | http://localhost:8080 | — |
| H2 Console | http://localhost:8080/h2-console | sa / (sem senha) |
| RabbitMQ UI | http://localhost:15672 | guest / guest |

> No H2 Console, use a JDBC URL: `jdbc:h2:mem:lanchonetedb`

---

## 📦 Estrutura de Pacotes

```
com.lanchonete/
├── config/          # Configuração do RabbitMQ (filas, exchange, bindings)
├── controller/      # Controllers REST (HTTP layer)
├── domain/
│   ├── entity/      # Entidades JPA (Produto, Pedido)
│   └── enums/       # StatusPedido, CategoriaProduto
├── dto/             # Request e Response DTOs
├── exception/       # Exceções customizadas + GlobalExceptionHandler
├── messaging/       # PedidoProducer + PedidoConsumer (RabbitMQ)
├── repository/      # Interfaces JPA
└── service/         # Regras de negócio
```

---

## 🔄 Fluxo de Mensageria

```
Cliente
  │
  ├─ POST /pedidos
  │     │
  │     ▼
  │  [Banco: RECEBIDO]
  │     │
  │     └─ RabbitMQ ──► fila.pedidos.novos
  │                            │
  │                     PedidoConsumer
  │                     (simula cozinha)
  │                            │
  │                     [Banco: EM_PREPARO]
  │                     [tempoEstimado = N*5min]
  │
  ├─ GET /pedidos/{id}
  │     └─ retorna { status: "EM_PREPARO", tempoPreparoEstimado: 15 }
  │
  ├─ POST /pedidos/{id}/pronto
  │     │
  │     ▼
  │  [Banco: PRONTO]
  │     │
  │     └─ RabbitMQ ──► fila.pedidos.prontos
  │                            │
  │                     PedidoConsumer
  │                     (notifica atendimento)
  │
  └─ POST /pedidos/{id}/entregue
        └─ [Banco: ENTREGUE]
```

---

## 📡 Endpoints

### Cardápio (público)
| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/cardapio` | Lista produtos disponíveis |
| GET | `/cardapio?categoria=BEBIDA` | Filtra por categoria |

### Produtos (administrativo)
| Método | Endpoint | Descrição |
|---|---|---|
| GET | `/produtos` | Lista todos (incluindo indisponíveis) |
| GET | `/produtos/{id}` | Busca por ID |
| POST | `/produtos` | Cria novo produto |
| PUT | `/produtos/{id}` | Atualiza produto |
| DELETE | `/produtos/{id}` | Remove produto |
| PATCH | `/produtos/{id}/disponibilidade?ativo=false` | Ativa/desativa |

### Pedidos
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/pedidos` | Cria novo pedido |
| GET | `/pedidos/{id}` | Consulta status e tempo estimado |
| GET | `/pedidos?status=EM_PREPARO` | Lista por status |
| POST | `/pedidos/{id}/pronto` | Marca como pronto (cozinha) |
| POST | `/pedidos/{id}/entregue` | Confirma entrega (atendimento) |

---

## 💡 Exemplos de Requisições

### Criar pedido
```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "produtoIds": [1, 3, 6],
    "observacoes": "sem cebola no lanche"
  }'
```

### Consultar status
```bash
curl http://localhost:8080/pedidos/1
# Resposta:
# {
#   "id": 1,
#   "status": "EM_PREPARO",
#   "tempoPreparoEstimado": 15,
#   "valorTotal": 43.90,
#   ...
# }
```

### Marcar como pronto (cozinha)
```bash
curl -X POST http://localhost:8080/pedidos/1/pronto
```

### Confirmar entrega
```bash
curl -X POST http://localhost:8080/pedidos/1/entregue
```

### Criar produto (admin)
```bash
curl -X POST http://localhost:8080/produtos \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "X-Tudo",
    "descricao": "O lanche completo",
    "preco": 29.90,
    "categoria": "LANCHE"
  }'
```

---

## 🚨 Tratamento de Erros

A API usa o padrão **RFC 9457 (Problem Details)** para respostas de erro:

```json
// 404 — Produto não encontrado
{
  "type": "https://lanchonete.api/erros/nao-encontrado",
  "title": "Recurso Não Encontrado",
  "status": 404,
  "detail": "Produto com id 999 não encontrado no cardápio",
  "timestamp": "2024-01-15T14:30:00Z"
}

// 400 — Erro de validação
{
  "type": "https://lanchonete.api/erros/validacao",
  "title": "Erro de Validação",
  "status": 400,
  "detail": "Um ou mais campos possuem valores inválidos.",
  "erros": {
    "produtoIds": "O pedido deve conter pelo menos um produto",
    "preco": "O preço deve ser positivo"
  }
}

// 422 — Regra de negócio violada
{
  "type": "https://lanchonete.api/erros/operacao-invalida",
  "title": "Operação Inválida",
  "status": 422,
  "detail": "Transição inválida: pedido está com status 'RECEBIDO'. Apenas pedidos EM_PREPARO podem ser marcados como PRONTO."
}
```

---

## 🧪 Executar Testes

```bash
# Todos os testes
./mvnw test

# Com relatório de cobertura
./mvnw test jacoco:report
```
