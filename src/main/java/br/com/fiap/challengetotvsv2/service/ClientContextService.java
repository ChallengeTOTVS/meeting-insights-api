package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.cliente.ClientContextResponseDto;
import br.com.fiap.challengetotvsv2.dto.cliente.ClientContextResponseDto.Fato;
import br.com.fiap.challengetotvsv2.dto.cliente.ClientFactSourceDto;
import br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.exception.ClienteNotFoundException;
import br.com.fiap.challengetotvsv2.exception.UsuarioAutenticadoNotFoundException;
import br.com.fiap.challengetotvsv2.repository.IClienteRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import static br.com.fiap.challengetotvsv2.dto.insights.IndividualAnalysisDto.*;

@Service
@RequiredArgsConstructor
public class ClientContextService {
    private final IClienteRepository clients;
    private final IReuniaoRepository meetings;
    private final IUsuarioRepository users;
    private final ObjectMapper mapper;
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ClientContextService.class);

    private record Candidate(UUID meetingId, LocalDateTime date, TipoFato type, String subject,
                             String description, SituacaoFato status) {}
    private record ValueKey(String description, SituacaoFato status) {}

    /** Any authenticated employee has the same client visibility as GET /cliente; meeting owners are not filters. */
    @Transactional(readOnly = true)
    public ClientContextResponseDto consultar(UUID clientId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        users.findByEmail(email).orElseThrow(UsuarioAutenticadoNotFoundException::new);
        if (!clients.existsById(clientId)) throw new ClienteNotFoundException(clientId);

        long completed = meetings.contarPorClienteEStatus(clientId, StatusReuniao.CONCLUIDA);
        List<ClientFactSourceDto> sources = meetings.buscarFatosCompartilhaveis(clientId, StatusReuniao.CONCLUIDA);
        Map<String, List<Candidate>> groups = new TreeMap<>();
        for (var source : sources) {
            if (source.analiseIndividualJson() == null || source.dataReuniao() == null) continue;
            try {
                var analysis = mapper.readValue(source.analiseIndividualJson(), IndividualAnalysisDto.class);
                var share = analysis == null ? null : analysis.fatosCompartilhaveis();
                if (share == null || share.estado() != Estado.CONCLUIDO || share.itens() == null) continue;
                Set<String> evidenceIds = analysis.evidencias() == null ? Set.of()
                        : analysis.evidencias().stream().filter(Objects::nonNull).map(Evidencia::id)
                            .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
                for (var fact : share.itens()) {
                    if (!valid(fact, evidenceIds)) continue;
                    String subject = fact.assunto().strip();
                    String key = fact.tipo().name() + "|" + normalize(subject);
                    groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Candidate(
                            source.reuniaoId(), source.dataReuniao(), fact.tipo(), subject,
                            fact.descricao().strip(), fact.situacao()));
                }
            } catch (Exception error) {
                // Old or malformed analysis is unavailable; never return its private JSON or transcript.
                LOGGER.warn("Fatos compartilháveis indisponíveis para reunião {}", source.reuniaoId());
            }
        }

        List<Fato> facts = new ArrayList<>();
        boolean conflicts = false;
        for (var candidates : groups.values()) {
            Set<UUID> meetingIds = new HashSet<>();
            Set<ValueKey> values = new HashSet<>();
            candidates.forEach(c -> { meetingIds.add(c.meetingId()); values.add(new ValueKey(c.description(), c.status())); });
            boolean divergent = meetingIds.size() > 1 && values.size() > 1;
            conflicts |= divergent;
            Map<ValueKey, List<LocalDateTime>> datesByValue = new LinkedHashMap<>();
            Candidate representative = candidates.getFirst();
            for (var candidate : candidates) {
                datesByValue.computeIfAbsent(new ValueKey(candidate.description(), candidate.status()), ignored -> new ArrayList<>())
                        .add(candidate.date());
            }
            for (var value : datesByValue.entrySet()) {
                facts.add(new Fato(representative.type().name(), representative.subject(), value.getKey().description(),
                        value.getKey().status().name(), value.getValue().stream().distinct().sorted().toList(), divergent));
            }
        }
        facts.sort(Comparator.comparing((Fato f) -> f.datasReuniao().getFirst())
                .thenComparing(Fato::tipo).thenComparing(Fato::assunto));
        String state = facts.isEmpty() ? "SEM_FATOS_DISPONIVEIS" : "RESUMO_DISPONIVEL";
        String message = completed == 0
                ? "Este cliente ainda não tem reuniões concluídas."
                : facts.isEmpty()
                    ? "Não há fatos confirmados disponíveis nas análises salvas. Reuniões antigas não foram reanalisadas automaticamente."
                    : conflicts
                        ? "Resumo baseado em fatos confirmados. Há informações diferentes sobre o mesmo assunto; confirme a condição vigente com o cliente."
                        : "Fatos e decisões registrados em reuniões concluídas. Consulte as datas para verificar quando cada item foi definido.";
        return new ClientContextResponseDto(clientId, completed, state, message, conflicts, facts);
    }

    private boolean valid(FatoCompartilhavel fact, Set<String> evidenceIds) {
        return fact != null && fact.tipo() != null && fact.situacao() != null
                && fact.assunto() != null && !fact.assunto().isBlank()
                && fact.descricao() != null && !fact.descricao().isBlank() && fact.descricao().length() <= 360
                && (fact.situacao() == SituacaoFato.DEFINIDO || fact.situacao() == SituacaoFato.PENDENTE
                    || fact.situacao() == SituacaoFato.CONCLUIDO)
                && fact.referencias() != null && !fact.referencias().isEmpty()
                && evidenceIds.containsAll(fact.referencias());
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }
}
