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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<UsuarioResponse> login (@RequestBody LoginRequest request, HttpServletRequest requisicao) {
        Usuario usuario = authService.login(request, requisicao.getRemoteAddr());
        String token = jwtService.gerarToken(usuario);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieSessao.criar(token, JwtService.VALIDADE).toString())
                .body(usuarioMapper.toResponse(usuario));
    }

    // O JavaScript não consegue apagar um cookie HttpOnly: quem encerra a sessão no navegador é a API.
    // Apagar o cookie não basta: uma cópia do token continuaria valendo até vencer. Por isso as sessões são encerradas
    // (em todos os aparelhos). Sem sessão válida, só o cookie é apagado
    @PostMapping("/logout")
    @Operation(summary = "Sair: encerra as sessões da conta e apaga o cookie")
    @SecurityRequirements
    public ResponseEntity<Void> logout (@AuthenticationPrincipal Usuario usuario) {
        if (usuario != null) {
            authService.encerrarSessoes(usuario.getId());
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieSessao.apagar().toString())
                .build();
    }
}
