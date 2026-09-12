package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.insights.HistoricalInsightsResponseDto;
import br.com.fiap.challengetotvsv2.exception.ReuniaoNotFoundException;
import br.com.fiap.challengetotvsv2.model.ClienteEntity;
import br.com.fiap.challengetotvsv2.model.ReuniaoEntity;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IInsightRepository;
import br.com.fiap.challengetotvsv2.repository.IReuniaoRepository;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalInsightsServiceTest {

    @Mock private IReuniaoRepository reuniaoRepository;
    @Mock private IUsuarioRepository usuarioRepository;
    @Mock private IInsightRepository insightRepository;
    @Mock private RagService ragService;
    @Mock private HistoricalAnalysisIaService historicalAnalysisIaService;

    @InjectMocks private HistoricalInsightsService historicalInsightsService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveBloquearBuscaHistoricaDeReuniaoDeOutroUsuario() {
        UsuarioEntity usuario = usuario();
        autenticar(usuario);
        UUID reuniaoDeOutroUsuario = UUID.randomUUID();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(reuniaoRepository.findByIdAndUsuarioId(reuniaoDeOutroUsuario, usuario.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> historicalInsightsService.gerarHistorico(reuniaoDeOutroUsuario))
                .isInstanceOf(ReuniaoNotFoundException.class);

        verifyNoInteractions(ragService, historicalAnalysisIaService);
    }

    @Test
    void deveRetornarSemHistoricoSemChamarOLlm() {
        UsuarioEntity usuario = usuario();
        ReuniaoEntity reuniao = reuniao(usuario);
        autenticar(usuario);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(reuniaoRepository.findByIdAndUsuarioId(reuniao.getId(), usuario.getId())).thenReturn(Optional.of(reuniao));
        when(insightRepository.findHistoricoComReuniaoPorClienteEUsuario(
                reuniao.getCliente().getId(), usuario.getId())).thenReturn(List.of());
        when(ragService.buscarContextoHistorico(reuniao)).thenReturn(List.of());

        HistoricalInsightsResponseDto resposta = historicalInsightsService.gerarHistorico(reuniao.getId());

        assertThat(resposta.historicoDisponivel()).isFalse();
        assertThat(resposta.analise()).isNull();
        assertThat(resposta.evidencias()).isEmpty();
        verifyNoInteractions(historicalAnalysisIaService);
    }

    @Test
    void deveExcluirReunioesPosterioresDaEvolucao() {
        var usuario = usuario(); var reuniao = reuniao(usuario); autenticar(usuario);
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(reuniaoRepository.findByIdAndUsuarioId(reuniao.getId(), usuario.getId())).thenReturn(Optional.of(reuniao));
        var past = reuniao(usuario); past.setDataReuniao(reuniao.getDataReuniao().minusDays(1));
        var future = reuniao(usuario); future.setDataReuniao(reuniao.getDataReuniao().plusDays(1));
        when(insightRepository.findHistoricoComReuniaoPorClienteEUsuario(reuniao.getCliente().getId(), usuario.getId()))
                .thenReturn(List.of(br.com.fiap.challengetotvsv2.model.InsightEntity.builder().reuniao(future).build(),
                        br.com.fiap.challengetotvsv2.model.InsightEntity.builder().reuniao(past).build()));
        when(ragService.buscarContextoHistorico(reuniao)).thenReturn(List.of());
        assertThat(historicalInsightsService.gerarHistorico(reuniao.getId()).evolucaoScores())
                .extracting(i -> i.reuniaoId()).containsExactly(past.getId());
        verifyNoInteractions(historicalAnalysisIaService);
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder().id(UUID.randomUUID()).email("usuario@totvs.com").build();
    }

    private ReuniaoEntity reuniao(UsuarioEntity usuario) {
        return ReuniaoEntity.builder()
                .id(UUID.randomUUID())
                .titulo("Reunião atual")
                .dataReuniao(LocalDateTime.of(2026, 9, 10, 11, 0))
                .transcricao("Cliente perguntou sobre integração.")
                .cliente(ClienteEntity.builder().id(UUID.randomUUID()).nome("Cliente A").empresa("Empresa A").build())
                .usuario(usuario)
                .build();
    }

    private void autenticar(UsuarioEntity usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario.getEmail(), null, List.of()));
    }
}
