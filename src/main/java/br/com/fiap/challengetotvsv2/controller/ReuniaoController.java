package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoRequestDto;
import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoResponseDto;
import br.com.fiap.challengetotvsv2.service.ReuniaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("reunioes")
public class ReuniaoController {

    private final ReuniaoService reuniaoService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReuniaoResponseDto cadastrarReuniao(@RequestBody @Valid ReuniaoRequestDto reuniaoRequestDto) {
        return reuniaoService.cadastrarReuniao(reuniaoRequestDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public List<ReuniaoResponseDto> listarReunioesDoUsuario() {
        return reuniaoService.listarMinhasReunioes();
    }

    @GetMapping("/{id}")
    public ReuniaoResponseDto buscarPorId(
            @PathVariable UUID id) {

        return reuniaoService.buscarReuniaoPorId(id);
    }
}
