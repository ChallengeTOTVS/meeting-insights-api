# Produtos, concorrentes e experiências

As novas gerações individuais e históricas incluem `produtosExperiencias` na mesma resposta de IA. O campo permanece opcional nos JSONs antigos, apresentados como **Informação não avaliada nesta versão**, sem geração automática.

O backend carrega `src/main/resources/catalogo-comercial.json`, seleciona apenas entradas com nomes/apelidos presentes nas transcrições/excertos enviados e relações cujas duas entradas foram selecionadas. A versão e o recorte completos são anexados pelo backend e persistidos junto ao resultado. Leituras usam os JSONs salvos, inclusive a versão original do catálogo, sem consultar IA.

O catálogo inicial contempla Protheus, Datasul, RM, Logix, Fluig e Winthor; SAP Business One, ERP Senior, ERP Sankhya, ERP Omie e Oracle NetSuite, além de entradas de fornecedor com produto não especificado. Produtos explicitamente citados fora desse conjunto podem ser analisados sem identificação de catálogo. Não é uma lista exaustiva.

Cada entrada contém fontes oficiais e datas de revisão. Relações de concorrência são **inferências comerciais por segmento**, com as fontes dos dois produtos; não comprovam comparação ou preferência do cliente. O catálogo não contém promessas de funcionalidades, preços ou condições comerciais.

Para revisar o catálogo, confira as páginas oficiais, atualize nomes/apelidos e fontes/datas, revise relações e incremente `versao`. Não altere snapshots já salvos. Apelidos ambíguos precisam de contexto na transcrição. Citar só SAP, Senior, Sankhya, Omie, Oracle ou TOTVS não identifica automaticamente um produto.

Observações preservam participante, relação, percepção e experiência separadamente. Trechos literais e referências sustentam contextos, aspectos, vantagens/dificuldades e critérios. Recomendações e oportunidades da IA são rotuladas separadamente das falas e das referências de catálogo. A validação local remove citações inexistentes e identidades sem menção; datas vêm dos metadados do backend. Mudanças exigem evidências do mesmo produto em reuniões distintas, não apenas chunks diferentes.

Na análise individual há uma seção própria. No histórico os produtos e concorrentes ficam em Relacionamento. Desempenho avalia a resposta do vendedor e oferece uma referência a Relacionamento. Resultados históricos cobrem os excertos recuperados, não necessariamente todas as falas de todas as reuniões.

Testes exercitam aliases, filtro de catálogo, empresa sem produto, citações falsas, participantes, percepção versus experiência, reuniões versus chunks, serialização e consulta sem nova IA. Essas verificações não substituem avaliação semântica da saída do modelo; as regras interpretativas estão em `ProductsAnalysisPrompt`.

