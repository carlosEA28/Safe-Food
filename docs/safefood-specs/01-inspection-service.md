# Inspection Service — Especificação SDD

**Missão:** manter checklists reutilizáveis, executar inspeções internas e registrar observações verificáveis. É a fonte de verdade das respostas e do estado da inspeção, **não** da resolução de pendências.

## Dependências
**Spring Initializr:** Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Flyway Migration, Spring for RabbitMQ, Spring Boot Actuator; Lombok opcional.
**`pom.xml`:** `org.flywaydb:flyway-database-postgresql`, `org.springdoc:springdoc-openapi-starter-webmvc-ui` (versão compatível), `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.testcontainers:rabbitmq`, `org.testcontainers:junit-jupiter`, `spring-rabbit-test`. RabbitMQ pode ser configurado só em INS-06.

## Modelo de dados e justificativa
| Entidade | Campos fundamentais | Por que existe |
|---|---|---|
| `ChecklistTemplate` | `id`, `name`, `description`, `active`, `createdAt` | Agrupar perguntas reutilizáveis, como higiene e armazenamento. |
| `ChecklistQuestion` | `id`, `templateId`, `text`, `category`, `position`, `severityIfFailed`, `active` | Determinar o que inspecionar e o risco configurado para resposta negativa. |
| `Inspection` | `id`, `templateId`, `templateNameSnapshot`, `inspectorId`, `status`, `startedAt`, `completedAt`, `notes`, `createdAt` | Representar uma inspeção de data específica. |
| `InspectionAnswer` | `id`, `inspectionId`, `questionId`, `questionTextSnapshot`, `categorySnapshot`, `severitySnapshot`, `result`, `observation`, `answeredAt`, `correctiveStatus` | Preservar respostas históricas mesmo após edição do checklist; espelhar a resolução externa. |
| `ProcessedEvent` | `eventId`, `processedAt` | Evitar processar duas vezes `corrective-action.completed`. |
| `OutboxEvent` | `id`, `eventType`, `payload`, `createdAt`, `publishedAt` | Publicação confiável dos eventos de não conformidade. |

**Enums:** `InspectionStatus = IN_PROGRESS, COMPLETED, CANCELED`; `AnswerResult = COMPLIANT, NON_COMPLIANT, NOT_APPLICABLE`; `Severity = LOW, MEDIUM, HIGH`; `CorrectiveStatus = NOT_REQUIRED, OPEN, COMPLETED`.

**Regras essenciais:** perguntas de um template são copiadas como snapshots ao iniciar inspeção; somente respostas de uma inspeção `IN_PROGRESS` são alteráveis; inspeção concluída fica imutável; `NON_COMPLIANT` exige observação; `NOT_APPLICABLE` não gera pendência; conclusão exige resposta a todas as perguntas (incluindo N/A); `correctiveStatus` é um espelho assíncrono, não modifica o resultado original.

## Features e tasks

### INS-01 — Fundação e persistência
**RF:** iniciar serviço isolado com migrations e health check. **RNF:** banco separado, validações e testes reproduzíveis.
- [ ] INS-01.1 Gerar projeto, configurar perfis `local`/`test`, PostgreSQL, Flyway e Actuator.
- [ ] INS-01.2 Criar migrations de `checklist_template` e `checklist_question` com FK, índices e posições únicas por template.
- [ ] INS-01.3 Implementar entidades, repositórios e estrutura controller/service/DTO/mapper.
- [ ] INS-01.4 Definir JSON de erros (`code`, `message`, `timestamp`) para validação e conflitos.
- [ ] INS-01.5 Testar migrations e persistência em Testcontainers.
**Aceite:** aplicação sobe do zero; migrations repetíveis; teste salva template + perguntas; campos inválidos produzem 400.

### INS-02 — Gerenciar modelos de checklist
**RF:** cadastrar, listar, consultar, editar e desativar modelos e questões. **RNF:** paginação e ordenação estável.
- [ ] INS-02.1 `POST /checklists`: nome e descrição; iniciar como ativo.
- [ ] INS-02.2 `POST /checklists/{id}/questions`: texto, categoria, posição, severidade de falha.
- [ ] INS-02.3 `GET /checklists` paginado; `GET /checklists/{id}` incluindo questões ordenadas.
- [ ] INS-02.4 `PUT /checklists/{id}` e `PUT /checklists/{id}/questions/{questionId}`.
- [ ] INS-02.5 `PATCH /checklists/{id}/deactivate`; desativar não pode apagar histórico.
- [ ] INS-02.6 Cobrir nomes vazios, posições duplicadas, checklist inexistente, criação em modelo inativo.
**Aceite:** um checklist com 3 perguntas aparece na ordem configurada; modelo inativo não pode iniciar nova inspeção.

### INS-03 — Abrir e responder inspeção
**RF:** iniciar inspeção de modelo ativo e responder perguntas. **RNF:** cópia imutável do texto e da severidade.
- [ ] INS-03.1 Criar tabelas `inspection`/`inspection_answer`, índices por status e data.
- [ ] INS-03.2 `POST /inspections` com `templateId`, `inspectorId`; criar snapshots das questões e estado `IN_PROGRESS`.
- [ ] INS-03.3 `PUT /inspections/{id}/answers/{answerId}` com `result` e `observation`.
- [ ] INS-03.4 Validar que `NON_COMPLIANT` exige observação não vazia.
- [ ] INS-03.5 `GET /inspections/{id}` com perguntas e respostas; `GET /inspections?status=&inspectorId=` paginado.
- [ ] INS-03.6 Testar alteração posterior do checklist sem alterar perguntas da inspeção antiga.
**Aceite:** respostas só podem ser alteradas durante inspeção em andamento; questões pertencentes a outra inspeção retornam 404.

### INS-04 — Conclusão e cancelamento
**RF:** concluir quando todas as perguntas forem respondidas; cancelar antes da conclusão.
- [ ] INS-04.1 `POST /inspections/{id}/complete`; validar cobertura total das perguntas.
- [ ] INS-04.2 Registrar `completedAt`, manter respostas imutáveis e calcular contagem de conformidades/não conformidades.
- [ ] INS-04.3 `POST /inspections/{id}/cancel` somente quando `IN_PROGRESS`.
- [ ] INS-04.4 Bloquear respostas, cancelamento ou nova conclusão após `COMPLETED`.
- [ ] INS-04.5 Testar casos válidos e transições ilegais.
**Aceite:** uma inspeção com item sem resposta não conclui (409); inspeção concluída retorna resumo e data.

### INS-05 — Consulta de resultados
**RF:** permitir histórico e visualização de não conformidades.
- [ ] INS-05.1 `GET /inspections/{id}/nonconformities` com `answerId`, descrição, severidade e status de correção.
- [ ] INS-05.2 `GET /inspections` filtrável por período, status e inspetor.
- [ ] INS-05.3 Construir `GET /inspections/{id}/summary` com total de perguntas, COMPLIANT, NON_COMPLIANT e N/A.
- [ ] INS-05.4 Testar filtros e paginação.
**Aceite:** uma inspeção com 2 conformes, 1 não conforme e 1 N/A retorna contagens corretas.

### INS-06 — Publicar não conformidades via RabbitMQ
**RF:** para cada resposta não conforme de inspeção concluída, emitir exatamente um evento lógico. **RNF:** persistência e publicação confiáveis.
- [ ] INS-06.1 Declarar exchange `safefood.events` (topic) e routing key `inspection.nonconformity.detected`.
- [ ] INS-06.2 No fechamento, gravar um OutboxEvent por resposta `NON_COMPLIANT`, na mesma transação local da conclusão.
- [ ] INS-06.3 Implementar publisher de outbox com confirmação de publicação e retry seguro.
- [ ] INS-06.4 Definir payload: `inspectionId`, `answerId`, `questionText`, `description`, `severity`.
- [ ] INS-06.5 Garantir `answerId` único como chave de negócio na consumidora; evitar duplicação em republicação.
- [ ] INS-06.6 Teste integração verificando publicação somente após conclusão.
**Aceite:** 2 itens não conformes => 2 eventos; reprocessar outbox não cria duas ações no consumidor.

### INS-07 — Receber conclusão das correções
**RF:** refletir status de pendência resolvida no relatório sem reescrever o resultado da inspeção.
- [ ] INS-07.1 Declarar fila própria com binding `corrective-action.completed`.
- [ ] INS-07.2 Consumir mensagem versionada contendo `actionId`, `inspectionId`, `answerId`, `completedAt`.
- [ ] INS-07.3 Marcar `correctiveStatus=COMPLETED` por `answerId`, registrando `eventId` processado.
- [ ] INS-07.4 Definir retry e DLQ para eventos malformados ou falhas repetidas.
- [ ] INS-07.5 Testar entrega duplicada e chegada de referência inexistente (DLQ com diagnóstico).
**Aceite:** resultado original NON_COMPLIANT permanece; apenas status da correção muda; duplicatas não alteram contadores.

## Fora do escopo
Pontuação normativa oficial, inspeções por sensores, multiempresa, anexos de fotos, login e dashboards avançados.

## Convenções SDD (obrigatórias)
- Implementar uma **feature por vez**, na sequência indicada. Antes de codificar: especificação curta do comportamento, contrato de entrada/saída, critérios de aceite e testes esperados.
- A IA não deve implementar features futuras, criar microserviços adicionais, autenticação, API Gateway, gRPC, Kubernetes ou integrações não especificadas.
- Stack: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway, Spring Validation, Spring AMQP, Testcontainers, OpenAPI. Usar UTC em `Instant`; IDs UUID; migrações Flyway; respostas de erro consistentes.
- Cada serviço tem banco de dados **exclusivo**. Nunca realizar JOIN nem acessar tabelas de outro microsserviço.
- Em cada feature: criar testes de domínio, testar casos de erro e documentar exemplo de requisição/resposta. Marcar `- [x]` apenas após testes passarem.
- Sem login no MVP: `inspectorId`, `responsibleId` e `completedBy` são UUIDs fornecidos pela API e não representam identidades verificadas. Não publicar o MVP na internet sem implementar autorização.
- Paginação nas listagens; validação Bean Validation e tratamento uniforme de erros HTTP 400/404/409; `409` para conflitos de negócio.
- Mensageria com exchange `safefood.events` do tipo **topic**; mensagens JSON versionadas com envelope `{eventId, eventType, version, occurredAt, aggregateId, payload}`. Consumidores devem ser idempotentes; após o fluxo básico, acrescentar retries/DLQ e publicação confiável por outbox para eventos críticos.
