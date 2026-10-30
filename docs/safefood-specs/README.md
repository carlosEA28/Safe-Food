# SafeFood — planejamento e especificações SDD

Sistema de **inspeções internas de boas práticas alimentares** para um único estabelecimento no MVP. **Não é sistema de certificação legal ou substituto de fiscalização sanitária.**

## Escopo
1. **Inspection Service (Java):** modelos de checklist, inspeções e respostas, sinalização de não conformidades.
2. **Corrective Action Service (Java):** pendências corretivas recebidas por evento, atribuição, execução e conclusão.
3. **Document Service (Java):** catálogo e upload/download de PDFs internos em MinIO.
4. **RAG Service (Python, fase 2):** ingestão de PDFs e perguntas com fontes, desenvolvido manualmente.

## Ordem de implementação
1. `01-inspection-service.md` — features INS-01 a INS-05, inicialmente sem RabbitMQ; depois INS-06.
2. `02-corrective-action-service.md` — features CA-01 a CA-04; então CA-05/CA-06 para integração RabbitMQ.
3. Finalizar integração INS-06/INS-07, testar ponta a ponta.
4. `03-document-service.md` — features DOC-01 a DOC-05; DOC-06 quando iniciar RAG.
5. `04-rag-service-python.md` — implementação **manual**, não por SDD automatizado.

## Composição da infraestrutura
`postgres-inspection`, `postgres-actions`, `postgres-documents` (podem ser databases isolados na mesma instância local, credenciais e schemas separados); RabbitMQ; MinIO (somente Document Service); Qdrant apenas na fase 2.

## Padrões de comunicação
- Cliente → cada microsserviço: HTTP REST, portas locais distintas; sem gateway no MVP.
- Inspection → Corrective Action: evento `inspection.nonconformity.detected`.
- Corrective Action → Inspection: evento `corrective-action.completed` (espelho de progresso, não muda respostas históricas).
- Document → RAG (fase 2): `document.created`, `document.deleted`.
- Não são necessárias chamadas REST diretas entre serviços no MVP; eventos e referências por UUID bastam.

## Contrato de evento padrão
```json
{
  "eventId":"4b95d57e-f2f4-450e-92f6-a812bc10d8c3",
  "eventType":"inspection.nonconformity.detected",
  "version":1,
  "occurredAt":"2026-10-30T15:00:00Z",
  "aggregateId":"e5a31c27-3c71-4c92-8e45-b208d7392281",
  "payload":{"inspectionId":"...","answerId":"...","questionText":"...","description":"...","severity":"HIGH"}
}
```

## Critérios gerais de pronto
- API documentada no Swagger; migrations aplicáveis do zero.
- Testes de unidade, integração PostgreSQL e mensageria quando houver.
- `docker compose up` inicia infraestrutura e serviços conforme documentação.
- Logs sem dados sensíveis; health checks disponíveis.
- Cada feature comprovada com casos de aceite escritos no documento do serviço.

## Fontes para futura base documental
- Cartilha oficial Anvisa: https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/alimentos/manuais-guias-e-orientacoes/cartilha-boas-praticas-para-servicos-de-alimentacao.pdf
- Texto da RDC 216/2004: https://anvisalegis.datalegis.net/action/ActionDatalegis.php?acao=abrirTextoAto&numeroAto=00000216&orgao=RDC%2FDC%2FANVISA%2FMS&tipo=RDC&valorAno=2004

Verificar vigência, normas complementares e condições de uso dos documentos antes de aplicá-los em contexto real. O modelo RAG não deve emitir laudos nem afirmar conformidade legal.
