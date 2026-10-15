package br.com.fiap.challengetotvsv2.config;

import br.com.fiap.challengetotvsv2.enums.Role;
import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import br.com.fiap.challengetotvsv2.repository.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Como o cadastro é fechado, alguém precisa ser o primeiro administrador.
 * Se ADMIN_EMAIL e ADMIN_SENHA estiverem no .env, esse usuário é criado (ou promovido) ao iniciar.
 */
@Component
@org.springframework.core.annotation.Order(1)
public class AdminInicialConfig implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminInicialConfig.class);

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;
    private final String nome;

    public AdminInicialConfig(IUsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                              @Value("${app.admin.email:}") String email,
                              @Value("${app.admin.senha:}") String senha,
                              @Value("${app.admin.nome:Administrador}") String nome) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        // Espaços copiados junto com o valor (comum ao colar no .env) são removidos.
        this.email = email == null ? null : email.strip();
        this.senha = senha == null ? null : senha.strip();
        this.nome = nome == null ? null : nome.strip();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            LOGGER.info("ADMIN_EMAIL/ADMIN_SENHA não definidos: nenhum administrador inicial criado.");
            return;
        }
        usuarioRepository.findByEmail(email).ifPresentOrElse(usuario -> {
            if (usuario.getRole() != Role.ADMIN) {
                usuario.setRole(Role.ADMIN);
                usuarioRepository.save(usuario);
                LOGGER.info("Usuário {} promovido a administrador.", email);
            }
        }, () -> {
            usuarioRepository.save(UsuarioEntity.builder()
                    .nome(nome)
                    .email(email)
                    .senha(passwordEncoder.encode(senha))
                    .role(Role.ADMIN)
                    .dataCriacao(LocalDateTime.now())
                    .build());
            LOGGER.info("Administrador inicial {} criado.", email);
        });
    }
}
