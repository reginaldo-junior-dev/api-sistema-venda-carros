package com.reginaldo.apisistemavendacarros.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Cookie que guarda o JWT no navegador.
 * HttpOnly: nenhum JavaScript lê o token, nem um script injetado na página (XSS).
 * SameSite=Lax: o navegador não manda o cookie em requisições que outros sites disparam (POST, PUT, DELETE).
 * Secure: só trafega por HTTPS (o navegador abre exceção para http://localhost).
 */
@Component
public class CookieSessao {

    public static final String NOME = "sessao";

    private final boolean seguro;

    public CookieSessao(@Value("${app.cookie.seguro:true}") boolean seguro) {
        this.seguro = seguro;
    }

    // Mesma duração do token: os dois vencem juntos
    public ResponseCookie criar(String token, Duration validade) {
        return base(token).maxAge(validade).build();
    }

    public ResponseCookie apagar() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> ler(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NOME.equals(cookie.getName()) && !cookie.getValue().isBlank())
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(seguro)
                .sameSite("Lax")
                .path("/");
    }
}
