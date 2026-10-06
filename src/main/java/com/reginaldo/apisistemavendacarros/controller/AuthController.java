package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.login.LoginRequest;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.mapper.UsuarioMapper;
import com.reginaldo.apisistemavendacarros.security.CookieSessao;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import com.reginaldo.apisistemavendacarros.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieSessao cookieSessao;
    private final UsuarioMapper usuarioMapper;

    // O token vai só no cookie HttpOnly: a resposta traz os dados da conta, nunca o token
    @PostMapping("/login")
    @Operation(summary = "Fazer login: grava o cookie da sessão e devolve a conta")
    @SecurityRequirements
    public ResponseEntity<UsuarioResponse> login (@RequestBody LoginRequest request) {
        Usuario usuario = authService.login(request);
        String token = jwtService.gerarToken(usuario);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieSessao.criar(token, JwtService.VALIDADE).toString())
                .body(usuarioMapper.toResponse(usuario));
    }

    // O JavaScript não consegue apagar um cookie HttpOnly: quem encerra a sessão no navegador é a API
    @PostMapping("/logout")
    @Operation(summary = "Sair: apaga o cookie da sessão")
    @SecurityRequirements
    public ResponseEntity<Void> logout () {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieSessao.apagar().toString())
                .build();
    }
}
