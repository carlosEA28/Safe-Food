# Corrective Action Service — Especificação SDD

**Missão:** receber não conformidades identificadas em inspeções e gerenciar seus planos de correção. É a fonte de verdade do estado das ações corretivas.

## Dependências
**Spring Initializr:** Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Flyway Migration, Spring for RabbitMQ, Spring Boot Actuator; Lombok opcional.
**`pom.xml`:** `org.flywaydb:flyway-database-postgresql`, `org.springdoc:springdoc-openapi-starter-webmvc-ui` compatível, `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.testcontainers:rabbitmq`, `org.testcontainers:junit-jupiter`, `spring-rabbit-test`.

## Entidades
| Entidade | Campos fundamentais | Justificativa |
|---|---|---|
| `CorrectiveAction` | `id`, `inspectionId`, `answerId` (unique), `title`, `description`, `severity`, `status`, `responsibleId` (opcional), `dueDate` (opcional), `resolutionNotes`, `completedBy`, `createdAt`, `updatedAt`, `completedAt` | Representa problema e solução; `answerId` liga ao fato origem sem acessar o banco de inspeções. |
| `ActionStatusHistory` | `id`, `actionId`, `fromStatus`, `toStatus`, `changedBy`, `reason`, `changedAt` | Auditoria de mudanças de status. |
| `ProcessedEvent` | `eventId` (unique), `processedAt` | Deduplicação de eventos RabbitMQ. |
| `OutboxEvent` | `id`, `eventType`, `payload`, `createdAt`, `publishedAt` | Garantia de que a conclusão emitirá evento mesmo após falha temporária do broker. |

**Enums:** `Status = OPEN, IN_PROGRESS, COMPLETED, CANCELED`; `Severity = LOW, MEDIUM, HIGH`.
**Regras:** toda ação nasce `OPEN`; somente `OPEN → IN_PROGRESS → COMPLETED`, `OPEN → CANCELED` ou `IN_PROGRESS → CANCELED`; só concluir com responsável, descrição de resolução não vazia e identificador de quem concluiu; item cancelado não emite `corrective-action.completed`; apenas uma ação por `answerId`.

## Features e tasks

### CA-01 — Setup e esquema
**RF:** armazenar e recuperar ações; **RNF:** banco isolado e validação.
- [ ] CA-01.1 Gerar Spring Boot + configurações dos perfis de teste/local.
- [ ] CA-01.2 Migration para `corrective_action`, `action_status_history`, `processed_event`, `outbox_event`.
- [ ] CA-01.3 Criar entidade, enum, repositórios, DTOs, serviço e controller.
- [ ] CA-01.4 Implementar validação e tradução de erros em 400/404/409.
- [ ] CA-01.5 Testar migrar base vazia e restrição UNIQUE em `answerId`.
**Aceite:** banco inicia sem dados; duas ações com mesmo `answerId` são recusadas.

### CA-02 — Consulta e organização
**RF:** listar e filtrar ações corretivas.
- [ ] CA-02.1 `GET /corrective-actions` com paginação, filtros `status`, `severity`, `responsibleId`, `inspectionId`, `dueBefore`.
- [ ] CA-02.2 `GET /corrective-actions/{id}` com detalhes.
- [ ] CA-02.3 `PATCH /corrective-actions/{id}/assignment` com `responsibleId` e `dueDate` (opcionais; dueDate não retroativa para novas atribuições).
- [ ] CA-02.4 Criar ordenação padrão por data de criação decrescente.
- [ ] CA-02.5 Testar filtros combinados e IDs inexistentes.
**Aceite:** lista filtra ações abertas de uma inspeção específica e retorna paginação estável.

### CA-03 — Execução e resolução
**RF:** iniciar e concluir ações com justificativa.
- [ ] CA-03.1 `POST /corrective-actions/{id}/start` exige responsável definido e estado OPEN.
- [ ] CA-03.2 `POST /corrective-actions/{id}/complete` exige `resolutionNotes` e `completedBy`.
- [ ] CA-03.3 Persistir transições em `ActionStatusHistory` junto à atualização de status.
- [ ] CA-03.4 `POST /corrective-actions/{id}/cancel` exige motivo e aceita OPEN/IN_PROGRESS.
- [ ] CA-03.5 Testar transições proibidas, conclusão duplicada e ausência de notas.
**Aceite:** não é possível concluir uma ação sem iniciar; histórico mostra quem mudou o status e quando.

### CA-04 — Indicadores simples
**RF:** consultar ações pendentes e vencidas.
- [ ] CA-04.1 `GET /corrective-actions/summary` com totais por status, criticidade e atrasadas.
- [ ] CA-04.2 Definir atrasada como `dueDate < hoje` e status OPEN/IN_PROGRESS (interpretar data usando timezone UTC documentada).
- [ ] CA-04.3 Criar testes para prazo igual a hoje e prazo passado.
**Aceite:** ação concluída nunca entra na contagem de atrasadas.

### CA-05 — Consumir eventos de não conformidade
**RF:** criar automaticamente ação para cada evento de inspeção. **RNF:** consumidor idempotente.
- [ ] CA-05.1 Declarar fila `safefood.corrective-actions.nonconformities`, binding `inspection.nonconformity.detected`.
- [ ] CA-05.2 Validar envelope de evento e payload obrigatório (`inspectionId`, `answerId`, `description`, `severity`, `questionText`).
- [ ] CA-05.3 Derivar `title` de `questionText`; gravar `CorrectiveAction` OPEN + `ProcessedEvent` na mesma transação.
- [ ] CA-05.4 Garantir deduplicação por `eventId` **e** unicidade de `answerId`.
- [ ] CA-05.5 Configurar retry limitado, DLQ, logs com `eventId` sem perder mensagens.
- [ ] CA-05.6 Testar mensagem duplicada e falha transitória do banco.
**Aceite:** publicar mesmo evento 3 vezes cria só uma ação; evento inválido vai à DLQ com diagnóstico.

### CA-06 — Publicar conclusão
**RF:** informar ao Inspection Service que uma ação foi concluída.
- [ ] CA-06.1 Na mesma transação que conclui ação, gravar `OutboxEvent` `corrective-action.completed`.
- [ ] CA-06.2 Payload deve conter `actionId`, `inspectionId`, `answerId`, `completedAt`.
- [ ] CA-06.3 Publicar com confirmação e retry seguro, marcar evento como publicado somente após confirmação.
- [ ] CA-06.4 Testar falha de RabbitMQ e posterior republicação sem alterar o estado do banco.
**Aceite:** conclusão persiste mesmo que broker esteja indisponível; eventual publicação torna status visível na inspeção.

## Fora do escopo
Notificações por email, assinatura digital, reabertura de ação concluída, fotos, integração com usuários e cron automático de lembrete.

## Convenções SDD (obrigatórias)
- Implementar uma **feature por vez**, na sequência indicada. Antes de codificar: especificação curta do comportamento, contrato de entrada/saída, critérios de aceite e testes esperados.
- A IA não deve implementar features futuras, criar microserviços adicionais, autenticação, API Gateway, gRPC, Kubernetes ou integrações não especificadas.
- Stack: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway, Spring Validation, Spring AMQP, Testcontainers, OpenAPI. Usar UTC em `Instant`; IDs UUID; migrações Flyway; respostas de erro consistentes.
- Cada serviço tem banco de dados **exclusivo**. Nunca realizar JOIN nem acessar tabelas de outro microsserviço.
- Em cada feature: criar testes de domínio, testar casos de erro e documentar exemplo de requisição/resposta. Marcar `- [x]` apenas após testes passarem.
- Sem login no MVP: `inspectorId`, `responsibleId` e `completedBy` são UUIDs fornecidos pela API e não representam identidades verificadas. Não publicar o MVP na internet sem implementar autorização.
- Paginação nas listagens; validação Bean Validation e tratamento uniforme de erros HTTP 400/404/409; `409` para conflitos de negócio.
- Mensageria com exchange `safefood.events` do tipo **topic**; mensagens JSON versionadas com envelope `{eventId, eventType, version, occurredAt, aggregateId, payload}`. Consumidores devem ser idempotentes; após o fluxo básico, acrescentar retries/DLQ e publicação confiável por outbox para eventos críticos.
