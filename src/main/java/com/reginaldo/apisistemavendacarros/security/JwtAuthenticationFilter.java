package com.reginaldo.apisistemavendacarros.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

           String token = authHeader.substring(7);

           try {
               String usuarioId = jwtService.extrairUsuarioId(token);

               UUID id = UUID.fromString(usuarioId);

               Usuario usuario = usuarioRepository.findById(id).orElse(null);

               if (usuario != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                List.of(
                                new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())
                                )
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
               }
           } catch (JWTVerificationException e) {
               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
               return;
           }

        }
           filterChain.doFilter(request, response);
    }
}
