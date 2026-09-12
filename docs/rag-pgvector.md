# RAG com PostgreSQL e pgvector

O `compose.yaml` usa `pgvector/pgvector:pg17`. Ao iniciar a aplicação, o Flyway executa `db/migration/V1__enable_pgvector_extensions.sql`, que instala as extensões necessárias. Para bancos já existentes, a configuração `baseline-on-migrate` cria uma baseline `0` e permite que a migration `V1` também seja aplicada.

Caso o usuário do banco não tenha permissão para instalar extensões, um administrador deve executar exatamente:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

A configuração `spring.ai.vectorstore.pgvector.initialize-schema=true` cria a tabela `vector_store` e o índice HNSW. Ela usa embeddings de 1536 dimensões, compatíveis com o modelo de embedding OpenAI padrão configurado pelo starter. Se o modelo de embedding for trocado por outro com dimensão diferente, ajuste `spring.ai.vectorstore.pgvector.dimensions` e recrie o vector store.
