package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.dto.auth.LoginRequestDto;
import br.com.fiap.challengetotvsv2.dto.auth.LoginResponseDto;
import br.com.fiap.challengetotvsv2.exception.CredenciaisInvalidasException;
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
        // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails existem.
        UsuarioEntity usuario = usuarioRepository.findByEmail(loginRequestDto.email().strip())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(loginRequestDto.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        String token = jwtService.gerarToken(usuario);
        return LoginResponseDto.builder()
                .token(token)
                .tipo("Bearer")
                .build();
    }
}
