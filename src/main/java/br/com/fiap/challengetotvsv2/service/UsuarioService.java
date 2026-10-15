package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioRequestDto;
import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioResponseDto;
import br.com.fiap.challengetotvsv2.enums.Role;
import br.com.fiap.challengetotvsv2.exception.EmailAlreadyExistsException;
import br.com.fiap.challengetotvsv2.exception.UsuarioAutenticadoNotFoundException;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import br.com.fiap.challengetotvsv2.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Só o administrador chama (regra no SecurityConfig). */
    public UsuarioResponseDto cadastrarUsuario(UsuarioRequestDto dto) {
        String email = dto.email().strip();
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome(dto.nome() == null || dto.nome().isBlank() ? null : dto.nome().strip())
                .email(email)
                .senha(passwordEncoder.encode(dto.senha()))
                .role(dto.role() == null ? Role.FUNCIONARIO : dto.role())
                .dataCriacao(LocalDateTime.now())
                .build();
        return toResponse(usuarioRepository.save(usuario));
    }

    public List<UsuarioResponseDto> listar() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(u -> u.getNome() == null ? u.getEmail() : u.getNome(),
                        String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponseDto usuarioAtual() {
        return toResponse(usuarioRepository.findByEmail(AuthUtils.emailAtual())
                .orElseThrow(UsuarioAutenticadoNotFoundException::new));
    }

    public UsuarioResponseDto alterarPerfil(UUID id, Role novoPerfil) {
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        if (usuario.getEmail().equalsIgnoreCase(AuthUtils.emailAtual()) && novoPerfil != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você não pode remover o seu próprio acesso de administrador.");
        }
        usuario.setRole(novoPerfil);
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioResponseDto toResponse(UsuarioEntity u) {
        return UsuarioResponseDto.builder()
                .id(u.getId())
                .nome(u.getNome())
                .email(u.getEmail())
                .role(u.getRole() == null ? Role.FUNCIONARIO : u.getRole())
                .build();
    }
}
