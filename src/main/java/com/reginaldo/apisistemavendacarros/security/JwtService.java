package com.reginaldo.apisistemavendacarros.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${JWT_SECRET}")
    private String jwtSecret;

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
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    public String extrairUsuarioId(String token) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        return JWT.require(algorithm)
                .build()
                .verify(token)
                .getSubject();
    }
}
