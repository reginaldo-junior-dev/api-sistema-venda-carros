package com.reginaldo.apisistemavendacarros.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    // Vale para o token e para o cookie que o guarda
    public static final Duration VALIDADE = Duration.ofHours(1);

    @Value("${JWT_SECRET}")
    private String jwtSecret;

    // Com um segredo curto, dá para descobri-lo por tentativa e erro e forjar um token de administrador
    @PostConstruct
    void validarSegredo () {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres. Gere um com: openssl rand -base64 48");
        }
    }

    public String gerarToken (Usuario usuario) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.create()
                .withSubject(usuario.getId().toString())
                .withClaim("perfil", usuario.getPerfil().name())
                .withIssuedAt(Instant.now())
                .withExpiresAt(dataExpiracao())
                .sign(algorithm);
    }

    public Instant dataExpiracao () {
        return Instant.now().plus(VALIDADE);
    }

    public String extrairUsuarioId(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.require(algorithm)
                .build()
                .verify(token)
                .getSubject();
    }
}
