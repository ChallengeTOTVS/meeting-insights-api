package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.cliente.ClienteRequestDto;
import br.com.fiap.challengetotvsv2.dto.cliente.ClienteResponseDto;
import br.com.fiap.challengetotvsv2.service.ClienteService;
import br.com.fiap.challengetotvsv2.service.ClientContextService;
import br.com.fiap.challengetotvsv2.dto.cliente.ClientContextResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;
    private final ClientContextService clientContextService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ClienteResponseDto cadastrarCliente(@Valid @RequestBody ClienteRequestDto clienteRequestDto) {
        return clienteService.cadastrarCliente(clienteRequestDto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ClienteResponseDto> listarTodosClientes() {
        return clienteService.listarTodos();
    }

    @GetMapping("/{id}/contexto")
    public ClientContextResponseDto consultarContexto(@PathVariable UUID id) {
        return clientContextService.consultar(id);
    }

    @GetMapping("/{id}")
    public ClienteResponseDto buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }
}
