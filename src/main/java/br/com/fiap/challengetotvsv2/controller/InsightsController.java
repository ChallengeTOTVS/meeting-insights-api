package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.insights.InsightsResponseDto;
import br.com.fiap.challengetotvsv2.dto.insights.HistoricalInsightsResponseDto;
import br.com.fiap.challengetotvsv2.service.PersistentHistoryService;
import br.com.fiap.challengetotvsv2.service.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reunioes")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;
    private final PersistentHistoryService historicalInsightsService;

    @GetMapping("/{reuniaoId}/insights")
    public InsightsResponseDto buscarInsights(
            @PathVariable UUID reuniaoId) {

        return insightsService.buscarInsightsPorReuniao(reuniaoId);
    }

    @GetMapping("/{reuniaoId}/insights/estado")
    public InsightsService.State consultarIndividual(@PathVariable UUID reuniaoId) {
        return insightsService.consultar(reuniaoId);
    }

    @PostMapping("/{reuniaoId}/insights/avaliacao")
    public InsightsResponseDto atualizarAvaliacao(@PathVariable UUID reuniaoId) {
        return insightsService.atualizarAvaliacao(reuniaoId);
    }

    @PostMapping("/{reuniaoId}/insights")
    @ResponseStatus(HttpStatus.CREATED)
    public InsightsResponseDto gerarInsights(
            @PathVariable UUID reuniaoId) {

        return insightsService.gerarInsights(reuniaoId);
    }

    @GetMapping({"/{reuniaoId}/insights/historico", "/{reuniaoId}/insights/historico/estado"})
    public PersistentHistoryService.State buscarInsightsHistoricos(
            @PathVariable UUID reuniaoId) {

        return historicalInsightsService.consultar(reuniaoId);
    }

    @PostMapping("/{reuniaoId}/insights/historico")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PersistentHistoryService.State iniciarHistorico(@PathVariable UUID reuniaoId) {
        return historicalInsightsService.iniciar(reuniaoId);
    }
}
