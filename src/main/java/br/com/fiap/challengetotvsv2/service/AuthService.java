package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.auth.LoginRequestDto;
import br.com.fiap.challengetotvsv2.dto.auth.LoginResponseDto;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDto autenticar(LoginRequestDto loginRequestDto) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(loginRequestDto.email()).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(loginRequestDto.senha(), usuario.getSenha())) {
            throw new RuntimeException("Email ou senha inválidos");
        }

        String token = jwtService.gerarToken(usuario);
        return LoginResponseDto.builder()
                .token(token)
                .tipo("Bearer")
                .build();
    }
}
