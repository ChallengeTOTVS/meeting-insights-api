package br.com.fiap.challengetotvsv2.controller;

import br.com.fiap.challengetotvsv2.dto.usuario.AlterarPerfilDto;
import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioRequestDto;
import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioResponseDto;
import br.com.fiap.challengetotvsv2.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Gestão de usuários: tudo exige ADMIN, exceto /usuarios/me (ver SecurityConfig). */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public UsuarioResponseDto cadastrarUsuario(@Valid @RequestBody UsuarioRequestDto dto) {
        return usuarioService.cadastrarUsuario(dto);
    }

    @GetMapping
    public List<UsuarioResponseDto> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/me")
    public UsuarioResponseDto usuarioAtual() {
        return usuarioService.usuarioAtual();
    }

    @PatchMapping("/{id}/perfil")
    public UsuarioResponseDto alterarPerfil(@PathVariable UUID id, @Valid @RequestBody AlterarPerfilDto dto) {
        return usuarioService.alterarPerfil(id, dto.role());
    }
}
