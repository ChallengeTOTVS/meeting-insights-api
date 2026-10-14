package br.com.fiap.challengetotvsv2.service;

final class ProductsAnalysisPrompt {
    private ProductsAnalysisPrompt() {}
    static final String RULES = """
        Na MESMA resposta gere produtosExperiencias; nenhuma chamada extra por produto.
        estado CONCLUIDO, SEM_MENCOES (somente após avaliar e não encontrar menções) ou EVIDENCIAS_INSUFICIENTES.
        catalogo=null: o backend grava a versão e o recorte utilizado.
        O catálogo comercial é referência para nomes, apelidos e concorrência, nunca prova de menção,
        uso ou qualidade. Suas relações por segmento são inferências comerciais, não comparações do cliente.
        Gere somente produtos/fornecedores explicitamente mencionados nas transcrições.
        Use catalogoId apenas para a entrada cujo nome/apelido foi explicitamente mencionado.
        Empresa sem produto identificado: nome="Produto não especificado", use entrada de fornecedor.
        Nunca deduza SAP Business One de "SAP", ERP Senior de "Senior" ou Protheus de "TOTVS".
        Nomes/apelidos ambíguos (como RM ou senior como cargo) exigem contexto de produto.
        Produtos fora do catálogo também podem ser registrados com catalogoId=null; fornecedor=null
        se não identificável. Não invente empresa, produto ou vínculo de concorrência.
        tipo TOTVS, CONCORRENTE ou OUTRO_FORNECEDOR conforme catálogo ou comparação explícita.
        evidencias próprias: IDs P1, P2...; referencia=ATUAL na individual e as REFERENCIAS recebidas
        na histórica; trecho contínuo LITERAL da transcrição com participante quando disponível.
        dataReuniao=null: preenchida pelo backend. participante literal ou null se não identificado.
        Cada Achado (contexto, aspectos, motivosConsideracao, vantagens, dificuldades, comparacoesCliente,
        obstaculosOportunidades, recomendacoesVendedor, mudancasPercepcao) contém texto e referencias a P1...
        Toda classificação e conclusão deve ser sustentada por essas evidências; preserve todas as listas.
        observacoes: uma por participante, época e experiência, sem fundir falas de pessoas diferentes.
        relacao: UTILIZA_ATUALMENTE, UTILIZOU_ANTERIORMENTE, AVALIANDO, RECEBEU_PROPOSTA,
        APENAS_MENCIONOU ou NAO_IDENTIFICADA. Use apenas a relação efetivamente sustentada.
        percepcao do CLIENTE: POSITIVA, NEGATIVA, MISTA, NEUTRA, NAO_IDENTIFICADA.
        experiencia real relatada: BOA, RUIM, MISTA ou NAO_RELATADA.
        "Parece uma boa solução": POSITIVA e NAO_RELATADA; não comprova uso.
        "Usamos e o suporte resolveu rapidamente": UTILIZA_ATUALMENTE e BOA.
        "O vendedor disse que é excelente": NAO_IDENTIFICADA e NAO_RELATADA.
        Pergunta, dúvida, preço ou comparação não implicam negatividade.
        Elogios e críticas da mesma experiência: MISTA e/ou MISTA conforme evidência; apresente ambos.
        Preserve separadamente relatos de uso passado e atual e divergências entre participantes.
        aspectos: funcionalidades, uso, implantação, integração, suporte, custo apenas quando relatados.
        Para concorrentes detalhe motivos de consideração/preferência, vantagens, dificuldades e critérios.
        comparacoesCliente: SOMENTE comparações expressas pelo cliente.
        obstaculosOportunidades: interpretação comercial da IA com justificativa baseada nas falas.
        recomendacoesVendedor: ações concretas da IA ligadas a necessidades/experiências (esclarecer,
        demonstrar, validar integração, detalhar suporte, investigar insatisfação, confirmar critérios).
        Nunca prometa funções, preços, integrações ou condições não confirmadas. Relato não prova qualidade geral.
        Na individual mudancasPercepcao=[]; na histórica mudanças exigem evidências do MESMO produto
        em reuniões distintas, mencionando as respectivas datas fornecidas, nunca inventadas.
        Não generalize satisfação com um produto para satisfação geral com TOTVS.
        Produtos, concorrentes e suas experiências têm lugar único em produtosExperiencias:
        não repita listas em relacionamento, concorrentes ou satisfacaoCliente.
        Desempenho avalia a RESPOSTA do vendedor aos relatos/comparações, referenciando este conteúdo
        em Relacionamento sem reproduzir listas. Recomendações comerciais não são fatos do cliente.
        """;
}

