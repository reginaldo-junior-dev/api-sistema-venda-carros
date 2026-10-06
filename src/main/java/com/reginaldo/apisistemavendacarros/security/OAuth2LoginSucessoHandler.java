package com.reginaldo.apisistemavendacarros.security;

import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSucessoHandler implements AuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final CookieSessao cookieSessao;

    // Endereço do front: o login termina na página /oauth/callback, que busca a conta e leva a pessoa adiante.
    // O token vai no cookie da sessão, nunca na URL
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User usuario = (OAuth2User) authentication.getPrincipal();

        String email = usuario.getAttribute("email");

        Usuario usuario1 = usuarioRepository.findByEmail(email).orElse(null);
        Usuario usuarioFinal;

        if (usuario1 != null) {
            usuarioFinal = usuario1;
        } else {
            Usuario novoUsuario = new Usuario();
            novoUsuario.setNomeCompleto(usuario.getAttribute("name"));
            novoUsuario.setEmail(usuario.getAttribute("email"));
            novoUsuario.setPerfil(PerfilUsuario.USUARIO);
            novoUsuario.setProvedor(ProvedorAutenticacao.GOOGLE);
            novoUsuario.setSenha(null);

           usuarioFinal = usuarioRepository.save(novoUsuario);
        }
           String token = jwtService.gerarToken(usuarioFinal);

           response.addHeader(HttpHeaders.SET_COOKIE, cookieSessao.criar(token, JwtService.VALIDADE).toString());
           response.sendRedirect(frontendUrl + "/oauth/callback");
    }
}
