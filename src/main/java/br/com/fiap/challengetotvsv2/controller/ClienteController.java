package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.cliente.ClienteRequestDto;
import br.com.fiap.challengetotvsv2.dto.cliente.ClienteResponseDto;
import br.com.fiap.challengetotvsv2.service.ClienteService;
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

    @GetMapping("/{id}")
    public ClienteResponseDto buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }
}
