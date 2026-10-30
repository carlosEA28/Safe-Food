# Document Service — Especificação SDD

**Missão:** armazenar e catalogar manuais de boas práticas, POPs (procedimentos operacionais padronizados), políticas e guias PDF. Não faz extração, embeddings ou perguntas na primeira fase.

## Dependências
**Spring Initializr:** Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Flyway Migration, Spring for RabbitMQ, Spring Boot Actuator; Lombok opcional.
**`pom.xml`:** `software.amazon.awssdk:s3` (via BOM da AWS SDK v2), `org.flywaydb:flyway-database-postgresql`, `org.springdoc:springdoc-openapi-starter-webmvc-ui` compatível, `spring-boot-starter-test`, `org.testcontainers:postgresql`, `org.testcontainers:rabbitmq`, `org.testcontainers:junit-jupiter`, `spring-rabbit-test`. MinIO é contêiner, não dependência Maven.

## Entidades
| Entidade | Campos fundamentais | Justificativa |
|---|---|---|
| `DocumentCategory` | `id`, `name` (unique), `description` | Agrupar documentos por assunto (HIGIENE, ARMAZENAMENTO, POP, CONTROLE_DE_PRAGAS). |
| `Document` | `id`, `categoryId`, `title`, `description`, `documentType`, `storageKey` (unique), `fileName`, `contentType`, `fileSize`, `sha256`, `sourceUrl` opcional, `status`, `createdAt`, `updatedAt` | Metadados de arquivo sem armazenar PDF no PostgreSQL. |
| `OutboxEvent` | `id`, `eventType`, `payload`, `createdAt`, `publishedAt` | Publicação eventual para RAG na fase 2. |

**Enums:** `DocumentType = GOOD_PRACTICES_MANUAL, SOP, GUIDELINE, OTHER`; `DocumentStatus = AVAILABLE, DELETED`. Documento eliminado logicamente deixa de ser baixável e deve gerar evento de exclusão quando integração existir.

## Features e tasks

### DOC-01 — Fundação e categorias
**RF:** iniciar catálogo documental. **RNF:** armazenamento independente.
- [ ] DOC-01.1 Criar projeto, Flyway, PostgreSQL, health check e Swagger.
- [ ] DOC-01.2 Migration de `document_category` e `document` com FK, índices por categoria e status.
- [ ] DOC-01.3 `POST /document-categories` e `GET /document-categories` (paginado).
- [ ] DOC-01.4 Criar DTOs com Bean Validation e erros HTTP padronizados.
- [ ] DOC-01.5 Testar duplicidade de categoria, nome vazio e consultas.
**Aceite:** categoria cadastrada pode ser usada por documentos futuros.

### DOC-02 — Upload de PDF no MinIO
**RF:** enviar PDF associado a categoria e metadados.
- [ ] DOC-02.1 Configurar cliente S3 com endpoint MinIO, bucket e credenciais por ambiente.
- [ ] DOC-02.2 `POST /documents` multipart: `file`, `title`, `description`, `categoryId`, `documentType`, `sourceUrl` opcional.
- [ ] DOC-02.3 Validar limite configurável de tamanho (MVP 20 MB), extensão e assinatura básica de PDF; rejeitar arquivo vazio.
- [ ] DOC-02.4 Calcular SHA-256; salvar sob chave UUID; registrar tamanho, tipo e origem no PostgreSQL.
- [ ] DOC-02.5 Tratar falha entre upload e INSERT: apagar objeto órfão em erro de gravação; não expor documento como AVAILABLE até sucesso.
- [ ] DOC-02.6 Testar upload com PDF válido e inválido (incluindo arquivo `.pdf` que não é PDF).
**Aceite:** upload válido cria registro e objeto; inválido retorna 400/413 sem registro AVAILABLE.

### DOC-03 — Consulta e download
**RF:** pesquisar e obter arquivos já enviados.
- [ ] DOC-03.1 `GET /documents` com filtro `categoryId`, `documentType`, `title`, paginação.
- [ ] DOC-03.2 `GET /documents/{id}` retorna metadados, nunca caminho físico ou credenciais.
- [ ] DOC-03.3 `GET /documents/{id}/download` faz streaming do objeto (ou URL assinada curta quando houver segurança implementada).
- [ ] DOC-03.4 Validar existência e status AVAILABLE; mapear ausência para 404.
- [ ] DOC-03.5 Testar bytes idênticos via hash no download.
**Aceite:** arquivo enviado pode ser baixado com SHA-256 idêntico.

### DOC-04 — Alteração e exclusão
**RF:** corrigir metadados e retirar documentos do catálogo.
- [ ] DOC-04.1 `PATCH /documents/{id}` atualiza título, descrição, categoria e tipo (não substitui arquivo).
- [ ] DOC-04.2 `DELETE /documents/{id}` realiza exclusão lógica idempotente; objeto pode ser removido em rotina separada após sucesso de evento.
- [ ] DOC-04.3 Impedir download e retorno em listagens padrão de documentos DELETED.
- [ ] DOC-04.4 Testar exclusão duplicada, edição de apagado e categoria inexistente.
**Aceite:** documento excluído não aparece na listagem e não pode ser baixado.

### DOC-05 — Operação local e integração de arquivos
**RF:** executar fluxo completo com MinIO e banco local.
- [ ] DOC-05.1 Adicionar serviço e MinIO no Docker Compose; bucket criado de maneira reproduzível.
- [ ] DOC-05.2 Testes integrados com PostgreSQL e armazenamento S3 compatível.
- [ ] DOC-05.3 Documentar variáveis (`S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`) e tamanho máximo.
- [ ] DOC-05.4 Documentar exemplos de upload, busca, download e delete no README.
**Aceite:** projeto sobe com Compose, aceita PDF e devolve arquivo sem alterar o conteúdo.

### DOC-06 — Eventos para o RAG (somente fase 2)
**RF:** notificar indexação e remoção de documentos. **RNF:** eventos confiáveis.
- [ ] DOC-06.1 Criar tabela `outbox_event`; no upload, gravar `document.created` no mesmo commit do registro AVAILABLE.
- [ ] DOC-06.2 Publicar payload `{documentId, documentType, title, categoryId, sha256}` sem incluir bytes, URL pública ou segredos.
- [ ] DOC-06.3 Em exclusão lógica, gravar e publicar `document.deleted` `{documentId}`.
- [ ] DOC-06.4 Disponibilizar `GET /internal/documents/{id}/download` somente em rede interna e com autenticação de serviço antes de qualquer deploy público; em desenvolvimento local, acesso restrito ao Docker network.
- [ ] DOC-06.5 Configurar retry, DLQ e testes de redelivery.
**Aceite:** ao enviar documento novo, RAG recebe ID e pode obtê-lo; ao excluir, RAG remove vetores associados.

## Cuidados de conteúdo
- Fontes oficiais públicas podem servir como referência, mas download público não concede automaticamente licença de redistribuição.
- Preferir upload pelo responsável e documentos com permissão adequada; validar origem e atualidade antes de uso real.
- PDFs podem conter instruções não confiáveis: conteúdo será tratado como dados pelo RAG, nunca como ordens de sistema.

## Convenções SDD (obrigatórias)
- Implementar uma **feature por vez**, na sequência indicada. Antes de codificar: especificação curta do comportamento, contrato de entrada/saída, critérios de aceite e testes esperados.
- A IA não deve implementar features futuras, criar microserviços adicionais, autenticação, API Gateway, gRPC, Kubernetes ou integrações não especificadas.
- Stack: Java 21, Spring Boot 3.5.x, Maven, PostgreSQL, Flyway, Spring Validation, Spring AMQP, Testcontainers, OpenAPI. Usar UTC em `Instant`; IDs UUID; migrações Flyway; respostas de erro consistentes.
- Cada serviço tem banco de dados **exclusivo**. Nunca realizar JOIN nem acessar tabelas de outro microsserviço.
- Em cada feature: criar testes de domínio, testar casos de erro e documentar exemplo de requisição/resposta. Marcar `- [x]` apenas após testes passarem.
- Sem login no MVP: `inspectorId`, `responsibleId` e `completedBy` são UUIDs fornecidos pela API e não representam identidades verificadas. Não publicar o MVP na internet sem implementar autorização.
- Paginação nas listagens; validação Bean Validation e tratamento uniforme de erros HTTP 400/404/409; `409` para conflitos de negócio.
- Mensageria com exchange `safefood.events` do tipo **topic**; mensagens JSON versionadas com envelope `{eventId, eventType, version, occurredAt, aggregateId, payload}`. Consumidores devem ser idempotentes; após o fluxo básico, acrescentar retries/DLQ e publicação confiável por outbox para eventos críticos.
