package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.insights.InsightsResponseDto;
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

    @GetMapping("/{reuniaoId}/insights")
    public InsightsResponseDto buscarInsights(
            @PathVariable UUID reuniaoId) {

        return insightsService.buscarInsightsPorReuniao(reuniaoId);
    }

    @PostMapping("/{reuniaoId}/insights")
    @ResponseStatus(HttpStatus.CREATED)
    public InsightsResponseDto gerarInsights(
            @PathVariable UUID reuniaoId) {

        return insightsService.gerarInsights(reuniaoId);
    }
}
