package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.chat.AssistantChatModelResponse;
import br.com.fiap.challengetotvsv2.dto.chat.AssistantChatRequestDto;
import br.com.fiap.challengetotvsv2.enums.Role;
import br.com.fiap.challengetotvsv2.enums.StatusReuniao;
import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import br.com.fiap.challengetotvsv2.model.InsightEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Chat com todas as reuniões da equipe, perfil Consulta e explicação do "por quê". */
class AssistantChatAllMeetingsTest {

    private final ChatClient ai = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final IUsuarioRepository users = mock(IUsuarioRepository.class);
    private final IReuniaoRepository meetings = mock(IReuniaoRepository.class);
    private final IInsightRepository insights = mock(IInsightRepository.class);
    private final AssistantChatService service =
            new AssistantChatService(ai, users, meetings, insights, JsonMapper.builder().build());

    private final UsuarioEntity vendedora = UsuarioEntity.builder().id(UUID.randomUUID())
            .nome("Ana Vendas").email("ana@empresa.com").role(Role.FUNCIONARIO).build();
    private final UsuarioEntity marketing = UsuarioEntity.builder().id(UUID.randomUUID())
            .nome("Bruno Marketing").email("bruno@empresa.com").role(Role.CONSULTA).build();
    private final ReuniaoEntity reuniaoNexora = reuniao("Renovação anual", "Nexora", vendedora,
            "Cliente: o suporte demorou três semanas para responder o chamado.");
    private final ReuniaoEntity reuniaoOutra = reuniao("Apresentação inicial", "Alfa Ltda", vendedora,
            "Cliente: gostamos da demonstração.");

    private static ReuniaoEntity reuniao(String titulo, String empresa, UsuarioEntity dono, String transcricao) {
        ClienteEntity cliente = ClienteEntity.builder().id(UUID.randomUUID()).nome("Contato " + empresa)
                .empresa(empresa).build();
        return ReuniaoEntity.builder().id(UUID.randomUUID()).titulo(titulo).cliente(cliente).usuario(dono)
                .dataReuniao(LocalDateTime.of(2026, 10, 1, 10, 0)).status(StatusReuniao.CONCLUIDA)
                .transcricao(transcricao).build();
    }

    private void logado(UsuarioEntity u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getEmail(), null, List.of()));
        when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void consultaDeOutraAreaVeReunioesDeTodosEExplicaOPorqueComFontes() {
        logado(marketing);
        InsightEntity insight = InsightEntity.builder().id(UUID.randomUUID()).reuniao(reuniaoNexora)
                .scoreChurn(0.8).nivelRisco("ALTO").justificativaRisco("Atraso recorrente no suporte")
                .sentimentoGeral("NEGATIVO").build();
        when(meetings.findAllComRelacoes()).thenReturn(List.of(reuniaoNexora, reuniaoOutra));
        when(insights.findAllComReuniao()).thenReturn(List.of(insight));
        when(ai.prompt().system(anyString()).user(anyString()).call().entity(AssistantChatModelResponse.class))
                .thenReturn(new AssistantChatModelResponse(true,
                        "O risco é alto por causa do suporte: \"o suporte demorou três semanas\".", List.of("R1")));

        var result = service.responder(new AssistantChatRequestDto("Por que a Nexora está com risco alto?", List.of()));

        assertThat(result.resposta()).contains("suporte");
        assertThat(result.fontes()).hasSize(1);
        assertThat(result.fontes().getFirst().reuniaoId()).isEqualTo(reuniaoNexora.getId());

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai.prompt(), atLeastOnce()).system(system.capture());
        verify(ai.prompt().system(anyString()), atLeastOnce()).user(user.capture());
        // Perfil Consulta recebe a observação de que não vê as telas.
        assertThat(system.getAllValues()).anyMatch(s -> s.contains("PERFIL DE QUEM PERGUNTA: Consulta"));
        String prompt = user.getAllValues().getLast();
        // As duas reuniões entram no índice, mesmo não sendo do usuário que perguntou.
        assertThat(prompt).contains("Nexora").contains("Alfa Ltda").contains("Atraso recorrente no suporte")
                .contains("suporte demorou três semanas");
        // Reuniões não são filtradas pelo dono.
        verify(meetings, never()).findByUsuarioId(any());
    }

    @Test
    void vendedorNaoRecebeObservacaoDePerfilConsulta() {
        logado(vendedora);
        when(meetings.findAllComRelacoes()).thenReturn(List.of(reuniaoNexora));
        when(insights.findAllComReuniao()).thenReturn(List.of());
        when(ai.prompt().system(anyString()).user(anyString()).call().entity(AssistantChatModelResponse.class))
                .thenReturn(new AssistantChatModelResponse(true, "Resposta.", List.of("R99")));

        var result = service.responder(new AssistantChatRequestDto("O que aconteceu na reunião com a Nexora?", List.of()));

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        verify(ai.prompt(), atLeastOnce()).system(system.capture());
        assertThat(system.getAllValues()).noneMatch(s -> s.contains("PERFIL DE QUEM PERGUNTA: Consulta"));
        // Código de reunião inexistente inventado pela IA é descartado.
        assertThat(result.fontes()).isEmpty();
    }

    @Test
    void falhaDaIaViraErro502ComMensagemClara() {
        logado(vendedora);
        when(meetings.findAllComRelacoes()).thenReturn(List.of(reuniaoNexora));
        when(insights.findAllComReuniao()).thenReturn(List.of());
        when(ai.prompt().system(anyString()).user(anyString()).call().entity(AssistantChatModelResponse.class))
                .thenThrow(new IllegalStateException("401 invalid api key"));

        assertThatThrownBy(() -> service.responder(new AssistantChatRequestDto("Qual o risco da Nexora?", List.of())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("OpenAI");
    }

    @Test
    void perguntaDeOutroAssuntoNemConsultaOBanco() {
        var result = service.responder(new AssistantChatRequestDto("Me passa uma receita de bolo de cenoura", List.of()));
        assertThat(result.resposta()).contains("Meeting Insights");
        verifyNoInteractions(users, meetings, insights);
    }

    @Test
    void perguntasDePorQueEDeConcorrentesPrecisamDoContextoDasReunioes() {
        assertThat(AssistantChatService.needsMeetingContext("Por que o cliente ficou insatisfeito?")).isTrue();
        assertThat(AssistantChatService.needsMeetingContext("Qual concorrente aparece mais?")).isTrue();
        assertThat(AssistantChatService.needsMeetingContext("Como cadastrar um cliente?")).isFalse();
    }

    @Test
    void perguntaComReunioesNoPluralPassaPeloFiltro() {
        // Bug encontrado no teste manual: "reuniões" (sem acento: reunioes) não casava com "reuniao".
        logado(vendedora);
        when(meetings.findAllComRelacoes()).thenReturn(List.of(reuniaoNexora));
        when(insights.findAllComReuniao()).thenReturn(List.of());
        when(ai.prompt().system(anyString()).user(anyString()).call().entity(AssistantChatModelResponse.class))
                .thenReturn(new AssistantChatModelResponse(true, "Melhore o retorno no prazo.", List.of("R1")));

        var result = service.responder(new AssistantChatRequestDto(
                "Considerando minhas ultimas reuniões, quais os principais pontos de melhora?", List.of()));

        assertThat(result.resposta()).isEqualTo("Melhore o retorno no prazo.");
    }

    @Test
    void minhasReunioesDetalhaSoAsDeQuemPergunta() {
        UsuarioEntity outro = UsuarioEntity.builder().id(UUID.randomUUID()).nome("Caio Outro")
                .email("caio@empresa.com").role(Role.FUNCIONARIO).build();
        ReuniaoEntity deOutro = reuniao("Kickoff Beta", "Beta SA", outro, "Cliente: tudo certo com o Beta.");
        logado(vendedora);
        when(meetings.findAllComRelacoes()).thenReturn(List.of(deOutro, reuniaoNexora));
        when(insights.findAllComReuniao()).thenReturn(List.of());
        when(ai.prompt().system(anyString()).user(anyString()).call().entity(AssistantChatModelResponse.class))
                .thenReturn(new AssistantChatModelResponse(true, "ok", List.of()));

        service.responder(new AssistantChatRequestDto("Quais os pontos de melhora nas minhas reuniões?", List.of()));

        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai.prompt().system(anyString()), atLeastOnce()).user(user.capture());
        String prompt = user.getAllValues().getLast();
        String detalhes = prompt.substring(prompt.indexOf("DETALHES_DAS_REUNIOES"), prompt.indexOf("HISTORICO_DA_CONVERSA"));
        assertThat(prompt).contains("QUEM_PERGUNTA: Ana Vendas");
        assertThat(detalhes).contains("Renovação anual").doesNotContain("Kickoff Beta");
        // O índice continua com todas, para perguntas gerais.
        assertThat(prompt).contains("Kickoff Beta");
    }

    @Test
    void outrosAssuntosContinuamBarrados() {
        assertThat(service.responder(new AssistantChatRequestDto("Qual a capital do Japão?", List.of())).resposta())
                .contains("Meeting Insights");
        verifyNoInteractions(users, meetings, insights);
    }
}
