package com.reginaldo.apisistemavendacarros.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// O token chega pelo cookie da sessão (navegador) ou pelo cabeçalho Authorization: Bearer (Swagger, Postman, testes)
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final RespostaErroSeguranca respostaErroSeguranca;
    private final CookieSessao cookieSessao;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                autenticar(authHeader.substring(7));
            } catch (JWTVerificationException e) {
                // Quem manda o cabeçalho escolheu esse token: o erro precisa aparecer
                respostaErroSeguranca.escrever(response, HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
                return;
            }
        } else {
            Optional<String> tokenDoCookie = cookieSessao.ler(request);
            if (tokenDoCookie.isPresent()) {
                try {
                    autenticar(tokenDoCookie.get());
                } catch (JWTVerificationException e) {
                    // O navegador manda o cookie em toda requisição: vencido, ele é apagado e a pessoa segue como visitante,
                    // senão até o catálogo, que é público, responderia 401
                    response.addHeader(HttpHeaders.SET_COOKIE, cookieSessao.apagar().toString());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String token) {
        UUID id = UUID.fromString(jwtService.extrairUsuarioId(token));

        // Perfil lido do banco a cada requisição: rebaixar um administrador vale na hora
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }
}
