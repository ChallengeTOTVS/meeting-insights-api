package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * Gera e lê o token JWT. O segredo vem da configuração (variável JWT_SECRET),
 * nunca do código, para que ninguém consiga forjar tokens lendo o repositório.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration validade;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours:8}") long horas) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou curto demais: defina no .env um segredo com pelo menos 32 caracteres.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validade = Duration.ofHours(horas);
    }

    public String gerarToken(UsuarioEntity usuario) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("role", usuario.getRole() == null ? "FUNCIONARIO" : usuario.getRole().name())
                .claim("nome", usuario.getNome())
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + validade.toMillis()))
                .signWith(key)
                .compact();
    }

    public String extrairEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
