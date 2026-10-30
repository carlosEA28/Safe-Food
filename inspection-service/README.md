# Inspection Service — INS-01

Requisitos: Java 21, Docker e Maven wrapper. Este serviço usa seu próprio banco PostgreSQL.

```sh
docker compose up -d
./mvnw spring-boot:run
```

A aplicação aplica a migration Flyway na inicialização. Consulte `GET http://localhost:8081/actuator/health` para verificar o estado. Para executar os testes de integração:

```sh
./mvnw test
```

Os testes fixam a API Docker 1.44 para compatibilidade do Testcontainers 1.21.x com versões recentes do daemon.

O perfil `local` é o padrão. `INSPECTION_DB_URL`, `INSPECTION_DB_USER` e `INSPECTION_DB_PASSWORD` permitem apontar para outro banco exclusivo do serviço. O perfil `test` obtém a conexão do Testcontainers.

Contrato mínimo de INS-01: `POST /checklists` cria um template sem perguntas. O gerenciamento completo vem em INS-02.

```http
POST /checklists HTTP/1.1
Content-Type: application/json

{"name":"Hygiene","description":"Kitchen hygiene"}
```

Resposta `201`:

```json
{"id":"<uuid>","name":"Hygiene","description":"Kitchen hygiene","active":true,"createdAt":"<UTC instant>"}
```

Campos inválidos retornam `400` no formato `{"code":"VALIDATION_ERROR","message":"name: must not be blank","timestamp":"<UTC instant>"}`. Restrições de integridade retornam `409` com `code` igual a `CONFLICT`.
