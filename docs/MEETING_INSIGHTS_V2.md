# Meeting Insights — análise individual e histórico

A análise individual usa somente a transcrição selecionada e inclui a avaliação do vendedor na mesma chamada de IA. O histórico compara apenas reuniões anteriores do mesmo cliente e usuário autorizado. Os dados existentes não são apagados nem regenerados automaticamente.

## Frontend

- Evolução: índices salvos de risco de perda do cliente, sentimento e resumo comparativo.
- Pendências e compromissos: assuntos não resolvidos, perguntas sem resposta, compromissos, responsável, prazo e situação.
- Relacionamento: prioridades, objeções recorrentes, concorrentes e fatores de satisfação/insatisfação.
- Desempenho: evolução da atuação e da resolução dos assuntos; link para a avaliação individual em “Nesta reunião”.
- Categorias aparecem em um único lugar; duplicatas textuais antigas tornam-se referências ao original. Dados originais permanecem salvos. Paráfrases semanticamente equivalentes não podem ser deduplicadas com garantia pelo comparador textual; o prompt também proíbe essa repetição.
- Na reunião selecionada, a tabela separa completude da resposta de resolução do assunto. Promessas de resposta posterior são acompanhamento, não resolução.
- Vendedor identificado pela transcrição. O usuário responsável pelo cadastro é exibido como “Cadastrado por”.
- Resumos e análise de desempenho em parágrafos; demais achados e recomendações em tópicos. Evidências expansíveis e links para pendências prioritárias.
- Texto secundário alterado de #62769c para #475b7a; fontes de leitura aumentadas em 1 px. Grades adaptáveis, tabela com rolagem horizontal própria e situações também expressas por rótulos.
- Campos antigos têm mensagem explícita. Atualização por IA tem botão próprio com aviso de tokens. Falhas, interrupções, processamento, ausência de geração e evidências insuficientes não são tratados como ausência de achados.

## Backend e persistência

- `GET /reunioes/{id}/insights`: lê resultado salvo, sem IA.
- `GET /reunioes/{id}/insights/estado`: consulta estado e resultado; sem IA, embeddings ou geração automática.
- `POST /reunioes/{id}/insights`: gera somente quando não há resultado/tentativa anterior; resultado existente é devolvido sem gerar novamente.
- `POST /reunioes/{id}/insights/avaliacao`: atualização explícita de registro antigo. Adiciona somente `analiseIndividualJson`, preservando resumo, risco, sentimento, listas e data de criação originais.
- `individual_analysis_job`: chave única `(reuniao_id, versao)`, reivindicada antes da IA com `ON CONFLICT DO NOTHING`. Protege múltiplos cliques, abas, requisições simultâneas e reinícios. Não há expiração que autorize outra geração. Uma tentativa com mais de 15 minutos é apresentada como interrompida, mas não é reenviada.
- Falhas ficam bloqueadas. Consultas e recarregamentos não fazem retries. O resultado salvo prevalece na consulta quando a persistência terminou antes de uma falha posterior.
- Persistência do histórico existente mantida; dados de reuniões posteriores foram excluídos também na consulta de evolução do serviço gerador.

## Prompts e evidências

- Instruções ficam na mensagem de sistema; transcrição e contexto histórico são dados não confiáveis. Pedidos contidos nesses dados não devem ser seguidos.
- A avaliação individual exige trechos literais e IDs. Validador verifica existência do trecho na transcrição, referências, enumerações, vendedor identificável, vínculos de interação, responsáveis/prazos e incompatibilidade entre resposta parcial/ausente e resolução.
- O histórico usa referências de fontes reais e inclui `ATUAL` para a reunião selecionada. Mudanças de risco/sentimento exigem referências às duas épocas. Referências inexistentes e duplicatas textuais são rejeitadas sem chamada de reparo ao modelo.
- Não são produzidas notas do vendedor. Notas antigas continuam persistidas; a tela privilegia a justificativa e informa ausência de critérios detalhados.
- Satisfação exige manifestação do cliente. O prompt proíbe inferi-la da quantidade de fala ou da simples existência de respostas e proíbe causalidade sem evidência.
- A checagem literal não prova interpretação semântica: conclusões continuam precisando de revisão humana pelas evidências exibidas. Nenhuma validação usou modelos pagos.

## Migração e execução

A migration aditiva `V3__individual_analysis.sql` adiciona a coluna nullable `insights.analise_individual_json` e a tabela de controle. Não executa UPDATE de resultados, DELETE ou regeneração. Há inicialização idempotente da mesma estrutura para a instalação atual, que já usa esse mecanismo para o histórico, e compatibilidade com `ddl-auto: update`.

O usuário deve reiniciar o backend na IDE para carregar classes, endpoints e estrutura novos. Nenhum backend foi iniciado ou encerrado durante o trabalho. Não é necessário reinstalar dependências do frontend. Para publicação do frontend, usar `npm run build`; o servidor de desenvolvimento pode recarregar os arquivos alterados.

## Verificações

- Testes unitários Java com Mockito: análise da primeira reunião, três graus de resposta, promessas, evidência inventada/inexistente, leitura sem IA, permissão, disputa pela geração, falha sem retry, atualização preservando valores antigos, serialização compatível e corte temporal do histórico.
- Testes Node: contratos GET seguros, cache, autorização, mapeamento dos estados, categorias exclusivas, referências de duplicatas, contraste e tamanhos de fonte.
- `npm run test:render`: renderização estática de componentes React com dados simulados, tabela, rótulos, links de evidência, legado, falha, interrupção e insuficiência de evidências.
- `npm run lint` e `npm run build`.
- A integração da migration com o banco real não foi executada, para preservar o ambiente do usuário.
- A inspeção visual em desktop e celular não pôde ser realizada: a ferramenta retornou inventário sem aplicativos ou navegadores. A responsividade foi revisada no código e a tabela verificada pela renderização estática; isso não substitui inspeção visual.

Para revisão visual sem backend real: `npm run build` e `npm run qa` no frontend. O servidor de fixtures responde localmente a todos os endpoints e nunca encaminha chamadas ao Spring Boot ou à IA. Login simulado: `qa@example.test` e uma senha qualquer de teste. A primeira reunião contém avaliação individual sem histórico; outras reuniões permitem conferir legado, processamento e erro.
