package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioRequestDto;
import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioResponseDto;
import br.com.fiap.challengetotvsv2.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UsuarioResponseDto cadastrarUsuario(@Valid @RequestBody UsuarioRequestDto usuarioRequestDTO) {

        return usuarioService.cadastrarUsuario(usuarioRequestDTO);
    }
}
