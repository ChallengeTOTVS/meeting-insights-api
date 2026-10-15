package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.gestao.GestaoResumoDto;
import br.com.fiap.challengetotvsv2.dto.gestao.GestaoReuniaoDto;
import br.com.fiap.challengetotvsv2.service.GestaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Endpoints do administrador. A regra "só ADMIN" está no SecurityConfig (/gestao/**). */
@RestController
@RequestMapping("/gestao")
@RequiredArgsConstructor
public class GestaoController {

    private final GestaoService gestaoService;

    @GetMapping("/resumo")
    public GestaoResumoDto resumo() {
        return gestaoService.resumo();
    }

    @GetMapping("/reunioes")
    public List<GestaoReuniaoDto> reunioes() {
        return gestaoService.listarReunioes();
    }

    /** Destrava uma análise que falhou ou ficou presa. Quem gera de novo é o vendedor dono da reunião. */
    @PostMapping("/reunioes/{id}/liberar-analise")
    public Map<String, Object> liberarAnalise(@PathVariable UUID id) {
        return gestaoService.liberarAnalise(id);
    }
}
