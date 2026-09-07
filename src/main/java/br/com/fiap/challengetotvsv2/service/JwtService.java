package br.com.fiap.challengetotvsv2.service;

import br.com.fiap.challengetotvsv2.model.UsuarioEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// essa calsse separa a responsabilidade de gerar o token
@Service
@RequiredArgsConstructor
public class JwtService {

    private final String secret = "segredo_qualquer";

    public String gerarToken(UsuarioEntity usuario) {

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("role", usuario.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+86400000))
                .signWith(getKey())
                .compact();
    }
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extrairEmail(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

    }
}
