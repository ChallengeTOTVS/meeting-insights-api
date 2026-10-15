package br.com.fiap.challengetotvsv2.config;

import br.com.fiap.challengetotvsv2.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Mantém a regra do banco "usuario.role só aceita estes valores" igual ao enum Role.
 *
 * Por quê: o Hibernate cria essa regra (check constraint) quando cria a tabela, mas o
 * ddl-auto: update NÃO a atualiza depois. Bancos criados antes do perfil CONSULTA
 * recusavam esse perfil com "violates check constraint usuario_role_check".
 *
 * Roda antes do AdminInicialConfig (@Order menor) e é idempotente: pode rodar sempre.
 */
@Component
@Order(0)
public class RoleConstraintConfig implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoleConstraintConfig.class);

    private final JdbcTemplate jdbc;

    public RoleConstraintConfig(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        String valores = Arrays.stream(Role.values())
                .map(r -> "'" + r.name() + "'")
                .collect(Collectors.joining(", "));
        try {
            jdbc.execute("ALTER TABLE usuario DROP CONSTRAINT IF EXISTS usuario_role_check");
            jdbc.execute("ALTER TABLE usuario ADD CONSTRAINT usuario_role_check CHECK (role IN (" + valores + "))");
            LOGGER.info("Perfis aceitos pela tabela usuario: {}", valores);
        } catch (RuntimeException e) {
            // Não impede o sistema de subir; só o perfil novo continuaria recusado pelo banco.
            LOGGER.warn("Não foi possível atualizar a regra de perfis da tabela usuario: {}", e.getMessage());
        }
    }
}
