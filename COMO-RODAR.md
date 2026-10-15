# Meeting Insights: como rodar tudo 

Este guia deixa o projeto rodando do zero no Windows. Siga na ordem.

## 0. O que você precisa ter instalado

| Ferramenta | Para quê | Como conferir |
|---|---|---|
| **JDK 25** | o `pom.xml` do Victor usa Java 25 | IntelliJ → File → Project Structure → SDK = 25 |
| **Docker Desktop** | sobe o PostgreSQL com pgvector | `docker --version` |
| **Node.js 20+** | roda o frontend | `node -v` |
| **Chave da OpenAI** com crédito | análises e chatbot | platform.openai.com → API keys |

> **Atenção às pastas.** Dentro de `D:\Faculdade\meeting-insights-api` existe uma subpasta
> `meeting-insights-api` com a **versão antiga** (Claude + embeddings locais). Não abra essa.
> O projeto certo é o `pom.xml` da **raiz** (`D:\Faculdade\meeting-insights-api\pom.xml`).
> Se quiser, mova a subpasta antiga para fora para não confundir a IDE.

## 1. Banco de dados (PostgreSQL + pgvector)

No terminal, dentro de `D:\Faculdade\meeting-insights-api`:

```powershell
docker compose up -d
```

Isso cria o contêiner `meeting-insights-postgres` (usuário `postgres`, senha `postgres`, banco `meeting_insights`).

**Já tinha esse contêiner da versão antiga?** O nome é o mesmo. A versão do Victor usa outra tabela de vetores
(`vector_store`, com 1536 dimensões, da OpenAI). Escolha uma opção:

- **Começar limpo (apaga os dados antigos):**
  ```powershell
  docker compose down -v
  docker compose up -d
  ```
- **Usar os dados do Victor** (o arquivo `meeting_insights.dump` na pasta):
  ```powershell
  docker cp meeting_insights.dump meeting-insights-postgres:/tmp/meeting_insights.dump
  docker exec meeting-insights-postgres pg_restore -U postgres -d meeting_insights --clean --if-exists --no-owner /tmp/meeting_insights.dump
  ```
  As senhas dos usuários do dump são as do Victor. Para entrar, use o admin do `.env` (passo 2). Ele é criado
  ou promovido a administrador automaticamente.

## 2. Arquivo `.env` do backend

Na raiz do backend (ao lado do `pom.xml`), copie o modelo e edite:

```powershell
copy .env.example .env
notepad .env
```

Preencha:
1. `OPENAI_API_KEY`: a chave real (`sk-...`);
2. `JWT_SECRET`: qualquer frase longa e aleatória com **32+ caracteres**. Para gerar uma no PowerShell:
   `[Convert]::ToBase64String([byte[]](1..48 | % { Get-Random -Maximum 256 }))`;
3. `ADMIN_EMAIL` / `ADMIN_SENHA`: o primeiro administrador.

O `.env` **não vai para o GitHub** (está no `.gitignore`). Se o `JWT_SECRET` mudar, quem estava logado precisa entrar de novo.

## 3. Rodar o backend (Spring Boot)

**Pelo IntelliJ:** File → Open → `D:\Faculdade\meeting-insights-api` → espere o Maven baixar as dependências →
abra `Challengetotvsv2Application` → botão ▶ Run.

**Pelo terminal:**
```powershell
.\mvnw.cmd spring-boot:run
```

Está certo quando aparecer no log:
```
Started Challengetotvsv2Application in ... seconds
Administrador inicial admin@meetinginsights.com criado.
```
O backend fica em `http://localhost:8080`.

## 4. Rodar o frontend (React + Vite)

Em `D:\Faculdade\meeting-insights-frontend`:

```powershell
npm install
npm run dev
```

Abra **http://127.0.0.1:5173**. O frontend **não precisa de `.env`**: por padrão ele já manda as chamadas `/api` para o
backend na porta 8080 (veja `vite.config.js`), então também não precisa configurar CORS.

## 5. Primeiro acesso (roteiro de demonstração)

1. Entre com o admin do `.env`. O **tour geral** abre sozinho no primeiro acesso, e cada tela mostra o próprio guia na primeira visita. O botão **Tutorial** no topo reabre o guia da tela atual.
2. Vá em **Usuários** e crie:
   - um **Vendedor** (ex.: `vendedor@empresa.com`);
   - um **Consulta**, para outras áreas (ex.: `marketing@empresa.com`).
3. Saia e entre como **vendedor**: cadastre um cliente, depois uma reunião com o `.txt` da transcrição
   (tem um exemplo em `reuniao_nexora_01.txt` no frontend), abra a reunião e clique em **Gerar análise**.
4. Clique no botão **Ajuda** (canto inferior direito) para abrir o assistente e pergunte, por exemplo:
   - *Por que a Nexora está com risco alto?*
   - *O que ficou combinado na última reunião?*
   - *Quais concorrentes mais aparecem?*
5. Saia e entre como **Consulta**: a pessoa só vê o Assistente. Se tentar abrir `/reunioes` ou `/equipe`, volta para o chat.

## 6. Ver as telas sem backend e sem gastar IA (modo demonstração)

```powershell
npm run build
npm run qa
```
Abra **http://127.0.0.1:5174**. Qualquer senha serve:
`qa@example.test` (vendedor) · `gestor@example.test` (gestor) · `consulta@example.test` (consulta).
O chat responde com textos fixos de exemplo.

## 7. Perfis e o que cada um acessa

| Perfil | Quem é | Telas | Chatbot |
|---|---|---|---|
| **Gestor** (`ADMIN`) | gestor comercial / administrador | Visão da equipe, Reuniões da equipe, Clientes, Usuários | todas as reuniões |
| **Vendedor** (`FUNCIONARIO`) | vendedor | Dashboard, Clientes, Reuniões (com o filtro "Só analisadas") | todas as reuniões |
| **Consulta** (`CONSULTA`) | outras áreas (marketing, produto, diretoria) | **só o Assistente** | todas as reuniões |

Não há cadastro público: só o gestor cria usuários e troca perfis. O perfil é lido do banco a cada requisição,
então a troca vale na hora, sem precisar sair e entrar.

## 8. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Chat responde "O assistente não conseguiu responder agora…" | chave da OpenAI errada ou sem crédito | conferir `OPENAI_API_KEY` no `.env` e reiniciar o backend |
| Backend não sobe: `JWT_SECRET ausente ou curto demais` | `.env` não foi lido | o `.env` precisa estar na raiz do backend e o IntelliJ precisa rodar com *Working directory* = essa pasta |
| `Connection refused` na porta 5432 | Docker parado | abrir o Docker Desktop e rodar `docker compose up -d` |
| Erro de versão do Java (`release 25`) | SDK errado | Project Structure → SDK 25 |
| Login funciona mas a tela diz "sessão expirou" | backend reiniciado com outro `JWT_SECRET` | sair e entrar de novo |
| Análise travada em "processando" | falha no meio da geração | o gestor vai em **Reuniões da equipe** → liberar nova tentativa |

---

## O que mudou nesta versão (resumo para o grupo)

Base: commit do Victor de 14/10 (`ac55af9`, "aprimora insights com RAG, análise comercial e assistente de IA").
**Nada foi enviado ao GitHub.**

### Backend
- **Novo perfil `CONSULTA`** para outras áreas: acessa só `/assistente/**` e `/usuarios/me`. Qualquer outra rota devolve 403.
- **Cadastro fechado:** `POST /usuarios` agora exige ADMIN (antes era público). Novos endpoints `GET /usuarios`,
  `GET /usuarios/me` e `PATCH /usuarios/{id}/perfil`. O admin não consegue tirar o próprio acesso.
- **Admin inicial** criado pelo `.env` (`AdminInicialConfig`).
- **Perfil lido do banco** a cada requisição (`JwtAuthenticationFilter`). O token também leva o `nome`.
- **Login com 401 correto** para e-mail ou senha errados (antes caía em erro 500).
- **Gestão da equipe:** `GET /gestao/resumo`, `GET /gestao/reunioes` e `POST /gestao/reunioes/{id}/liberar-analise`.
  Produtos e concorrentes vêm de `produtosExperiencias`, o formato novo do Victor.
- **Admin pode ler** qualquer reunião, análise e histórico (sem gerar; gerar continua só com o dono).
- **Chatbot (`POST /assistente/chat`) reescrito:**
  - enxerga **todas as reuniões** da plataforma, não só as do usuário logado;
  - explica o **porquê**: a resposta usa a justificativa do risco, as conclusões com os **trechos literais**
    (evidências) e o início da transcrição;
  - aceita perguntas gerais ("quais clientes…", "qual concorrente…") por meio de um índice de todas as reuniões;
  - devolve `fontes` (as reuniões citadas), que o frontend mostra como links;
  - adapta a linguagem quando quem pergunta é do perfil Consulta;
  - se a IA falhar, devolve 502 com uma mensagem clara;
  - **bug corrigido:** a versão anterior procurava a análise num campo `analiseIndividual` que não existe no JSON
    salvo. Por isso o chat nunca enxergava a análise detalhada.
- **Análise histórica mais completa:**
  - o histórico agora manda todas as reuniões anteriores do cliente (até 24 trechos, em ordem de data), em vez de 8 trechos "parecidos";
  - os trechos ganharam códigos curtos (H1, H2...) no lugar de IDs de 40 caracteres que a IA copiava errado;
  - uma referência inválida não apaga mais a conclusão inteira;
  - quem falou cada trecho é lido da própria transcrição ("Nome: fala").
- Testes: os 70 testes do Victor continuam passando, e entraram mais 8 (chat com todas as reuniões, perfil
  Consulta, falha da IA, gestão de usuários).

### Frontend
Base: frontend do Victor de 14/10 (`58ada2e`, "aprimora exibição dos insights históricos"), com o seguinte por cima:
- **Perfis e rotas:** cada perfil cai na própria tela inicial (gestor em Visão da equipe, vendedor no Dashboard,
  consulta no Assistente). Rotas de outro perfil redirecionam para a tela inicial.
- **Cadastro público removido** (`/cadastro` volta para o login). Quem cria usuários é o gestor, na tela **Usuários**,
  que permite escolher entre os perfis Vendedor, Gestor e Consulta e trocar o perfil de alguém pela lista.
- **Telas do gestor:** Visão da equipe, Reuniões da equipe (com "Liberar nova tentativa") e Usuários. O gestor abre
  qualquer reunião em **modo leitura**, sem os botões de gerar análise.
- **Chat:**
  - vendedor e gestor usam o chat flutuante do Victor (botão **Ajuda**), que agora mostra links para as reuniões citadas;
  - o perfil Consulta usa o assistente em página inteira (`/assistente`), com sugestões de perguntas;
  - **bug corrigido no chat flutuante:** o histórico ia com o campo `role`, mas o backend espera `papel`, então toda
    pergunta voltava erro 400.
- **Tutorial em duas camadas:**
  - **Tour geral** (um por perfil): abre no primeiro login e apresenta o menu.
  - **Guia de cada tela:** abre na primeira visita. A tela da reunião tem duas versões: antes da análise (como gerar) e
    depois (como ler resumo, resultado, perguntas, compromissos, produtos e concorrentes, desempenho e histórico).
  - O botão **Tutorial** abre o guia da tela atual, e dele dá para ir ao tour geral.
- O item **Insights** saiu do menu do vendedor (era a mesma lista de Reuniões filtrada). No lugar, Reuniões tem o botão **Só analisadas**; links antigos para `/insights` continuam funcionando.
- `npm run qa` agora tem os perfis `gestor@example.test` e `consulta@example.test`, a visão do gestor e um chat simulado.

> **Aviso para o Victor:** a pasta do frontend tem `tmp/` (backup antigo de classes do backend) e `output/` (um PDF pessoal).
> Elas não estão no `.gitignore`. Não faça commit delas.
