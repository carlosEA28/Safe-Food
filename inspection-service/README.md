# Inspection Service — INS-01 e INS-02

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

`POST /checklists` cria um modelo ativo sem perguntas:

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

## Gerenciamento de checklists

| Método | Caminho | Resultado |
|---|---|---|
| `POST` | `/checklists/{id}/questions` | Cria questão; `201` |
| `GET` | `/checklists?page=0&size=20` | Lista modelos, mais recentes primeiro; `size` de 1 a 100 |
| `GET` | `/checklists/{id}` | Modelo com questões em ordem de posição |
| `PUT` | `/checklists/{id}` | Atualiza nome e descrição |
| `PUT` | `/checklists/{id}/questions/{questionId}` | Atualiza questão |
| `PATCH` | `/checklists/{id}/deactivate` | Desativa modelo sem apagar dados |
| `PATCH` | `/checklists/{id}/questions/{questionId}/deactivate` | Desativa questão sem apagar dados |

Exemplo de questão:

```http
POST /checklists/<id>/questions HTTP/1.1
Content-Type: application/json

{"text":"Are surfaces clean?","category":"Hygiene","position":1,"severityIfFailed":"HIGH"}
```

Resposta `201`:

```json
{"id":"<uuid>","text":"Are surfaces clean?","category":"Hygiene","position":1,"severityIfFailed":"HIGH","active":true}
```

`GET /checklists/{id}` retorna os campos do modelo e `questions` como lista ordenada. A listagem retorna `content`, `page`, `size`, `totalElements` e `totalPages`; a ordem é `createdAt DESC, id DESC` e não aceita ordenação arbitrária. Posição repetida ou criação de questão em modelo inativo retorna `409`; recurso inexistente retorna `404`. Questões desativadas continuam visíveis no detalhe para preservar o cadastro. A verificação de modelo ativo ao abrir uma inspeção pertence à INS-03, quando esse endpoint for implementado.
