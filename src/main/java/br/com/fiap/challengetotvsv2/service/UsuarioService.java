package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioRequestDto;
import br.com.fiap.challengetotvsv2.dto.usuario.UsuarioResponseDto;
import br.com.fiap.challengetotvsv2.enums.Role;
import br.com.fiap.challengetotvsv2.exception.EmailAlreadyExistsException;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponseDto cadastrarUsuario(UsuarioRequestDto usuarioRequestDto) {

        if(usuarioRepository.existsByEmail(usuarioRequestDto.email())){
            throw new EmailAlreadyExistsException(usuarioRequestDto.email());
        }

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome(usuarioRequestDto.nome())
                .email(usuarioRequestDto.email())
                .senha(passwordEncoder.encode(usuarioRequestDto.senha()))
                .role(Role.FUNCIONARIO)
                .build();

        UsuarioEntity usuarioSalvo =  usuarioRepository.save(usuario);

        return UsuarioResponseDto.builder()
                .id(usuarioSalvo.getId())
                .nome(usuarioSalvo.getNome())
                .email(usuarioSalvo.getEmail())
                .role(usuarioSalvo.getRole())
                .build();
    };

}
