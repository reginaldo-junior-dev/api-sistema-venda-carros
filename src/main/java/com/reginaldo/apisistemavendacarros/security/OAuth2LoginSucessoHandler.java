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

        OAuth2User usuarioGoogle = (OAuth2User) authentication.getPrincipal();

        // A conta é localizada pelo e-mail: sem a confirmação do Google, nada prova que a pessoa é dona dele.
        // Volta ao front sem cookie, como no login recusado
        if (!Boolean.TRUE.equals(usuarioGoogle.getAttribute("email_verified"))) {
            response.sendRedirect(frontendUrl + "/oauth/callback");
            return;
        }

        String email = usuarioGoogle.getAttribute("email");
        Usuario usuario = usuarioRepository.findByEmail(email)
                .map(this::assumirConta)
                .orElseGet(() -> criarConta(usuarioGoogle));

        String token = jwtService.gerarToken(usuario);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieSessao.criar(token, JwtService.VALIDADE).toString());
        response.sendRedirect(frontendUrl + "/oauth/callback");
    }

    // O cadastro com senha não confirma o e-mail: alguém poderia cadastrar o endereço de outra pessoa antes dela
    // e continuar entrando com a própria senha depois que a dona começasse a usar a conta pelo Google.
    // Com o e-mail confirmado pelo Google, a senha deixa de valer e quem estava logado com ela é derrubado
    private Usuario assumirConta(Usuario usuario) {
        if (usuario.getSenha() == null) {
            return usuario;
        }
        usuario.setSenha(null);
        usuario.setProvedor(ProvedorAutenticacao.GOOGLE);
        usuario.encerrarSessoes();
        return usuarioRepository.save(usuario);
    }

    private Usuario criarConta(OAuth2User usuarioGoogle) {
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNomeCompleto(usuarioGoogle.getAttribute("name"));
        novoUsuario.setEmail(usuarioGoogle.getAttribute("email"));
        novoUsuario.setPerfil(PerfilUsuario.USUARIO);
        novoUsuario.setProvedor(ProvedorAutenticacao.GOOGLE);
        return usuarioRepository.save(novoUsuario);
    }
}
