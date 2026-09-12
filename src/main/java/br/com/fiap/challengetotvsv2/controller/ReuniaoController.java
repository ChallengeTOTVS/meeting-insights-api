package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoRequestDto;
import br.com.fiap.challengetotvsv2.dto.reuniao.ReuniaoResponseDto;
import br.com.fiap.challengetotvsv2.service.ReuniaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reunioes")
public class ReuniaoController {

    private final ReuniaoService reuniaoService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReuniaoResponseDto cadastrarReuniao(
            @RequestPart("dados") @Valid ReuniaoRequestDto reuniaoRequestDto,
            @RequestPart("arquivo") MultipartFile arquivo
    ) throws IOException {
        System.out.println("ENTROU NO ENDPOINT MULTIPART");

        return reuniaoService.cadastrarReuniao(reuniaoRequestDto, arquivo);
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

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{id}/rag/indexacao")
    public void indexarParaRag(@PathVariable UUID id) {
        reuniaoService.indexarReuniaoParaRag(id);
    }
}
