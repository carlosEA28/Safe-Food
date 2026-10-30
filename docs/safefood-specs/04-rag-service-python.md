# RAG Service — Especificação para implementação manual (Python)

**Fase 2 — iniciar somente após os três serviços Java funcionarem.** Objetivo: responder dúvidas de boas práticas internas usando **exclusivamente evidências dos PDFs cadastrados**. Não substituir profissional técnico, análise jurídica ou autoridade sanitária.

## Stack sugerida
Python 3.12+, FastAPI, Uvicorn, Pydantic Settings, PyMuPDF, `langchain-text-splitters` (opcional; pode implementar chunker manual), embeddings via `langchain-openai` ou SDK do provedor, Qdrant (`qdrant-client`), `httpx`, `pika` (ou aio-pika se usar fluxo async), `pytest`.

## Dados próprios
- `DocumentIndex`: `documentId`, `sha256`, `indexedAt`, `status` (`PENDING`, `INDEXING`, `READY`, `FAILED`, `DELETED`), `chunkCount`, `error` — pode ser SQLite/Postgres próprio ou armazenamento simples persistente no MVP.
- `Chunk` (Qdrant payload): `documentId`, `title`, `categoryId`, `documentType`, `pageNumber`, `chunkIndex`, `text`, `sha256`, `embeddingModel`.
- `ProcessedEvent`: `eventId`, `processedAt` para entregas repetidas de RabbitMQ.

## Contratos
- `POST /ask` request: `{ "question": "Como organizar o armazenamento de alimentos?", "categoryId": "uuid opcional", "topK": 5 }`.
- Response: `{ "answer": "...", "sources": [{"documentId":"...","title":"...","pageNumber":3,"excerpt":"..."}], "grounded": true }`.
- Se insuficiente: `grounded=false`, resposta explícita de insuficiência, `sources=[]`.
- `GET /health`; `GET /documents/{documentId}/index-status` para observabilidade.
- Sem chat history, agentes, Tavily, tools ou múltiplos índices no MVP.

## Features com tasks **manuais**

### RAG-01 — API e estrutura
- [ ] Criar projeto Python, ambiente virtual, `.env.example` e estrutura `app/api`, `app/ingestion`, `app/retrieval`, `app/config`.
- [ ] Configurar FastAPI, logs e `GET /health`.
- [ ] Escrever testes com pytest e request client.
**Aceite:** API sobe, testes passam, variáveis obrigatórias validadas.

### RAG-02 — Extração local dos PDFs
- [ ] Usar 2–3 PDFs com uso permitido e extração de texto confiável.
- [ ] Extrair conteúdo página a página com PyMuPDF, guardando índice real da página.
- [ ] Ignorar páginas vazias e registrar arquivos sem texto extraível como falha de extração; OCR fora do MVP.
- [ ] Criar testes para PDF válido, vazio e corrompido.
**Aceite:** saída preserva `documentId`, título, número da página e texto correspondente.

### RAG-03 — Chunks e embeddings
- [ ] Dividir texto em chunks configuráveis (ex.: 700–1000 caracteres com overlap 100–150; validar empiricamente).
- [ ] Evitar chunks sem conteúdo; preservar página, documento e categoria.
- [ ] Gerar embeddings por lote com modelo configurado; registrar nome e dimensão do embedding.
- [ ] Testar reprocessamento com mesmo documento/sha256 sem criar chunks duplicados.
**Aceite:** mesma entrada => quantidade de chunks estável; payload de todos inclui metadados para citar fonte.

### RAG-04 — Indexação no Qdrant
- [ ] Criar collection com dimensão e métrica compatíveis com o modelo de embedding.
- [ ] Usar IDs estáveis por `documentId`/índice de chunk para upsert idempotente.
- [ ] Criar índice filtrável para `documentId` e `categoryId` quando necessário.
- [ ] Implementar remoção de todos os vetores por `documentId`.
- [ ] Testar indexar, consultar, reindexar e excluir.
**Aceite:** reindexação não multiplica resultados; exclusão impede que documento volte nas buscas.

### RAG-05 — Retrieval e resposta fundamentada
- [ ] `POST /ask` converte pergunta em embedding e consulta Qdrant com `topK` validado (ex.: 1–10).
- [ ] Aplicar filtros por categoria somente quando informados; não misturar documentos não autorizados quando autenticação for acrescentada.
- [ ] Montar contexto com trechos recuperados e fontes numeradas.
- [ ] System prompt: responder em português, usar somente conteúdo recuperado, não inventar procedimentos e citar documento/página; se insuficiente, admitir limite.
- [ ] Implementar regra de abstenção e avaliação de relevância em conjunto de perguntas conhecido (não confiar só no score de similaridade).
- [ ] Retornar fontes estruturadas; testar pergunta respondível e pergunta sem evidência.
**Aceite:** resposta rastreável aos trechos; pergunta fora do corpus gera aviso de ausência de fundamento.

### RAG-06 — Integração com Document Service
- [ ] Consumir `document.created` na fila própria com idempotência por `eventId` e `documentId+sha256`.
- [ ] Buscar PDF via endpoint interno protegido, usando `httpx` com timeouts e retries delimitados.
- [ ] Indexar documento e atualizar status `READY`/`FAILED`.
- [ ] Consumir `document.deleted`, remover vetores e impedir reindexação por evento antigo (lidar com ordenação/versionamento).
- [ ] Implementar DLQ para falhas definitivas e reprocessamento manual de falhas.
- [ ] Teste ponta a ponta: enviar PDF no Java → indexar no Python → perguntar → excluir → não recuperar mais.
**Aceite:** nenhum acesso direto ao PostgreSQL/MinIO de outros serviços; eventos repetidos não duplicam índices.

### RAG-07 — Avaliação e robustez
- [ ] Construir dataset de 15–20 perguntas: respondíveis, ambíguas, fora de contexto e com resposta ausente.
- [ ] Medir recall@k dos trechos relevantes, taxa de abstenção adequada e qualidade de citações.
- [ ] Registrar tempos de extração, embedding, retrieval e geração; logs sem dados pessoais sensíveis.
- [ ] Documentar custos de embedding/LLM e limites de tamanho.
**Aceite:** métricas reproduzíveis e exemplos de falhas conhecidos documentados; nenhuma alegação de conformidade oficial automática.

## Instrução de integração
O RAG não precisa conhecer inspeções ou ações corretivas para funcionar. Na versão 1 responde perguntas sobre documentos; no futuro uma API pode receber a descrição de uma não conformidade como pergunta, sem mudar bancos de dados dos microsserviços Java.

## Referências de documentos
- Cartilha Anvisa https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/alimentos/manuais-guias-e-orientacoes/cartilha-boas-praticas-para-servicos-de-alimentacao.pdf
- RDC 216/2004 https://anvisalegis.datalegis.net/action/ActionDatalegis.php?acao=abrirTextoAto&numeroAto=00000216&orgao=RDC%2FDC%2FANVISA%2FMS&tipo=RDC&valorAno=2004

