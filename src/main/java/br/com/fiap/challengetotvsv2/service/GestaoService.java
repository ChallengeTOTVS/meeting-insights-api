package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.gestao.GestaoResumoDto;
import br.com.fiap.challengetotvsv2.dto.gestao.GestaoResumoDto.*;
import br.com.fiap.challengetotvsv2.dto.gestao.GestaoReuniaoDto;
import br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto;
import br.com.fiap.challengetotvsv2.dto.insights.ProductsAnalysisDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import br.com.fiap.challengetotvsv2.model.InsightEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IClienteRepository;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Visão do administrador: agrega reuniões e análises de toda a equipe.
 * Só lê o que já está salvo — nenhuma chamada de IA acontece aqui.
 * Para o volume de um protótipo, calcular em memória é simples e suficiente.
 */
@Service
@RequiredArgsConstructor
public class GestaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GestaoService.class);

    private final IReuniaoRepository reuniaoRepository;
    private final IInsightRepository insightRepository;
    private final IClienteRepository clienteRepository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    /** Reunião + análise (se houver) + análise individual já convertida do JSON. */
    private record Linha(ReuniaoEntity reuniao, InsightEntity insight, IndividualAnalysisDto analise) {
        Integer risco() {
            return insight == null || insight.getScoreChurn() == null ? null
                    : (int) Math.round(insight.getScoreChurn() * 100);
        }
        int pendencias() {
            if (analise == null || analise.interacoes() == null) return 0;
            return (int) analise.interacoes().stream()
                    .filter(i -> i.situacao() == IndividualAnalysisDto.Situacao.PENDENTE
                            || i.situacao() == IndividualAnalysisDto.Situacao.ACOMPANHAMENTO)
                    .count();
        }
        List<ProductsAnalysisDto.Produto> produtos() {
            if (analise == null || analise.produtosExperiencias() == null
                    || analise.produtosExperiencias().produtos() == null) return List.of();
            return analise.produtosExperiencias().produtos();
        }
        int oportunidades() {
            return produtos().stream()
                    .mapToInt(p -> p.obstaculosOportunidades() == null ? 0 : p.obstaculosOportunidades().size())
                    .sum();
        }
    }

    @Transactional(readOnly = true)
    public List<GestaoReuniaoDto> listarReunioes() {
        return linhas().stream().map(l -> {
            var r = l.reuniao();
            return new GestaoReuniaoDto(r.getId(), r.getTitulo(), r.getDataReuniao(), r.getCliente().getId(),
                    r.getCliente().getNome(), r.getCliente().getEmpresa(), r.getUsuario().getId(),
                    nome(r.getUsuario()), r.getStatus(), l.risco(),
                    l.insight() == null ? null : l.insight().getNivelRisco(),
                    l.insight() == null ? null : l.insight().getSentimentoGeral(), l.pendencias());
        }).toList();
    }

    @Transactional(readOnly = true)
    public GestaoResumoDto resumo() {
        List<Linha> linhas = linhas();
        List<Linha> analisadas = linhas.stream().filter(l -> l.insight() != null).toList();

        // Última reunião analisada de cada cliente = situação atual da conta.
        Map<UUID, Linha> ultimaPorCliente = new LinkedHashMap<>();
        analisadas.stream()
                .sorted(Comparator.comparing((Linha l) -> l.reuniao().getDataReuniao(),
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .forEach(l -> ultimaPorCliente.putIfAbsent(l.reuniao().getCliente().getId(), l));

        List<Conta> contas = ultimaPorCliente.values().stream().map(l -> {
            var r = l.reuniao();
            return new Conta(r.getCliente().getId(), r.getCliente().getNome(), r.getCliente().getEmpresa(),
                    nome(r.getUsuario()), r.getUsuario().getId(), r.getId(), r.getTitulo(), r.getDataReuniao(),
                    l.risco(), l.insight().getNivelRisco(), l.insight().getSentimentoGeral(),
                    l.pendencias(), l.oportunidades());
        }).sorted(Comparator.comparing(Conta::risco, Comparator.nullsLast(Comparator.reverseOrder()))).toList();

        // Por vendedor.
        Map<UUID, List<Linha>> porVendedor = linhas.stream()
                .collect(Collectors.groupingBy(l -> l.reuniao().getUsuario().getId(), LinkedHashMap::new, Collectors.toList()));
        List<Vendedor> vendedores = porVendedor.values().stream().map(lista -> {
            UsuarioEntity u = lista.getFirst().reuniao().getUsuario();
            List<Conta> contasDele = contas.stream().filter(c -> u.getId().equals(c.vendedorId())).toList();
            return new Vendedor(u.getId(), nome(u), u.getEmail(), lista.size(),
                    (int) lista.stream().filter(l -> l.insight() != null).count(),
                    contasDele.size(),
                    (int) contasDele.stream().filter(c -> "ALTO".equals(c.nivelRisco())).count(),
                    media(contasDele.stream().map(Conta::risco).toList()),
                    contasDele.stream().mapToInt(Conta::pendenciasAbertas).sum(),
                    lista.stream().mapToInt(Linha::oportunidades).sum());
        }).sorted(Comparator.comparing(Vendedor::contasRiscoAlto).reversed()).toList();

        // Produtos e concorrentes vêm da seção "produtosExperiencias" da análise individual.
        Map<String, SinalAcumulado> concorrentes = new HashMap<>();
        Map<String, SinalAcumulado> produtos = new HashMap<>();
        List<OportunidadeItem> oportunidades = new ArrayList<>();
        for (Linha l : analisadas) {
            String cliente = l.reuniao().getCliente().getEmpresa();
            for (ProductsAnalysisDto.Produto p : l.produtos()) {
                String contexto = relacaoPrincipal(p);
                if (p.tipo() == ProductsAnalysisDto.Tipo.TOTVS) acumular(produtos, p.nome(), cliente, contexto);
                else acumular(concorrentes, p.nome(), cliente, contexto);
                Optional.ofNullable(p.obstaculosOportunidades()).orElse(List.of()).forEach(o -> oportunidades.add(
                        new OportunidadeItem(l.reuniao().getId(), l.reuniao().getTitulo(), cliente,
                                nome(l.reuniao().getUsuario()), l.reuniao().getDataReuniao(), o.texto(),
                                p.tipo() == null ? null : p.tipo().name(), p.nome())));
            }
        }
        oportunidades.sort(Comparator.comparing(OportunidadeItem::data,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Objeções: campo de texto, um item por linha. Agrupa textos iguais (após normalizar).
        Map<String, SinalAcumulado> objecoes = new HashMap<>();
        for (Linha l : analisadas) {
            String texto = l.insight().getObjecoes();
            if (texto == null) continue;
            for (String linha : texto.split("\\R")) {
                String item = linha.replaceFirst("^[\\s\\-•*\\d.)]+", "").trim();
                if (item.length() > 3) acumular(objecoes, item, l.reuniao().getCliente().getEmpresa(), null);
            }
        }

        int contasRiscoAlto = (int) contas.stream().filter(c -> "ALTO".equals(c.nivelRisco())).count();
        var totais = new Totais(linhas.size(), analisadas.size(),
                (int) linhas.stream().filter(l -> l.insight() == null && l.reuniao().getStatus() == StatusReuniao.PENDENTE).count(),
                (int) linhas.stream().filter(l -> l.reuniao().getStatus() == StatusReuniao.ERRO).count(),
                (int) clienteRepository.count(), contas.size(), contasRiscoAlto,
                media(contas.stream().map(Conta::risco).toList()),
                contas.stream().mapToInt(Conta::pendenciasAbertas).sum(),
                oportunidades.size());

        return new GestaoResumoDto(totais, contas, vendedores, top(concorrentes, 10), top(produtos, 10),
                oportunidades.stream().limit(20).toList(), top(objecoes, 8), LocalDateTime.now());
    }

    /**
     * Destrava uma análise individual ou histórica que falhou ou ficou presa (mais de 15 min "processando").
     * Não apaga nenhuma análise concluída; o vendedor dono da reunião precisa gerar de novo.
     */
    @Transactional
    public Map<String, Object> liberarAnalise(UUID id) {
        ReuniaoEntity reuniao = reuniaoRepository.findComRelacoesById(id)
                .orElseThrow(() -> new ReuniaoNotFoundException(id));
        String travadas = "(status = 'ERRO' OR (status = 'PROCESSANDO' AND criado_em < CURRENT_TIMESTAMP - INTERVAL '15 minutes'))";
        boolean individual = jdbc.update("DELETE FROM individual_analysis_job WHERE reuniao_id = ? AND " + travadas, id) > 0;
        boolean historico = jdbc.update("DELETE FROM historical_analysis_job WHERE reuniao_id = ? AND " + travadas, id) > 0;
        if (insightRepository.findByReuniaoId(id).isEmpty() && reuniao.getStatus() != StatusReuniao.PENDENTE) {
            reuniao.setStatus(StatusReuniao.PENDENTE);
            reuniaoRepository.save(reuniao);
            individual = true;
        }
        return Map.of("analiseIndividual", individual, "historico", historico,
                "mensagem", individual || historico
                        ? "Nova tentativa liberada. O vendedor responsável pode gerar a análise novamente."
                        : "Não havia tentativa com falha para liberar.");
    }

    // ------------------------------------------------------------------ apoio

    private List<Linha> linhas() {
        Map<UUID, InsightEntity> insightPorReuniao = insightRepository.findAllComReuniao().stream()
                .collect(Collectors.toMap(i -> i.getReuniao().getId(), i -> i, (a, b) -> a));
        return reuniaoRepository.findAllComRelacoes().stream().map(r -> {
            InsightEntity insight = insightPorReuniao.get(r.getId());
            return new Linha(r, insight, ler(insight));
        }).toList();
    }

    private IndividualAnalysisDto ler(InsightEntity insight) {
        if (insight == null || insight.getAnaliseIndividualJson() == null) return null;
        try {
            return mapper.readValue(insight.getAnaliseIndividualJson(), IndividualAnalysisDto.class);
        } catch (RuntimeException e) {
            LOGGER.warn("Análise individual da reunião {} não pôde ser lida: {}", insight.getReuniao().getId(), e.getMessage());
            return null;
        }
    }

    /** Relação mais "forte" que algum participante declarou com o produto (usa, avalia, recebeu proposta...). */
    private static String relacaoPrincipal(ProductsAnalysisDto.Produto p) {
        if (p.observacoes() == null) return null;
        return p.observacoes().stream().map(ProductsAnalysisDto.Observacao::relacao).filter(Objects::nonNull)
                .filter(r -> r != ProductsAnalysisDto.Relacao.NAO_IDENTIFICADA)
                .min(Comparator.naturalOrder()).map(Enum::name).orElse(null);
    }

    private static String nome(UsuarioEntity u) {
        return u.getNome() == null || u.getNome().isBlank() ? u.getEmail() : u.getNome();
    }

    private static Integer media(List<Integer> valores) {
        var validos = valores.stream().filter(Objects::nonNull).toList();
        return validos.isEmpty() ? null
                : (int) Math.round(validos.stream().mapToInt(Integer::intValue).average().orElse(0));
    }

    private static final class SinalAcumulado {
        String nome;
        int mencoes;
        final Set<String> clientes = new LinkedHashSet<>();
        final Map<String, Integer> contextos = new LinkedHashMap<>();
    }

    private static void acumular(Map<String, SinalAcumulado> mapa, String nome, String cliente, String contexto) {
        if (nome == null || nome.isBlank()) return;
        String chave = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        var s = mapa.computeIfAbsent(chave, k -> new SinalAcumulado());
        if (s.nome == null) s.nome = nome.trim();
        s.mencoes++;
        if (cliente != null) s.clientes.add(cliente);
        if (contexto != null) s.contextos.merge(contexto, 1, Integer::sum);
    }

    private static List<Sinal> top(Map<String, SinalAcumulado> mapa, int limite) {
        return mapa.values().stream()
                .sorted(Comparator.comparingInt((SinalAcumulado s) -> s.mencoes).reversed())
                .limit(limite)
                .map(s -> new Sinal(s.nome, s.mencoes, List.copyOf(s.clientes), Map.copyOf(s.contextos)))
                .toList();
    }
}
