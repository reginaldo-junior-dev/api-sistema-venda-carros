package com.reginaldo.apisistemavendacarros.security;

import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Fim do login Google: a pessoa volta ao front, com o token no sucesso e sem ele na falha
class OAuth2LoginHandlersTest {

    private static final String FRONT = "https://patio.vercel.app";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final OAuth2LoginSucessoHandler sucesso = new OAuth2LoginSucessoHandler(usuarioRepository, jwtService);
    private final OAuth2LoginFalhaHandler falha = new OAuth2LoginFalhaHandler();

    @BeforeEach
    void configurarFront() {
        ReflectionTestUtils.setField(sucesso, "frontendUrl", FRONT);
        ReflectionTestUtils.setField(falha, "frontendUrl", FRONT);
    }

    private static TestingAuthenticationToken loginGoogle(String nome, String email) {
        OAuth2User usuarioGoogle = new DefaultOAuth2User(List.of(), Map.of("name", nome, "email", email), "email");
        return new TestingAuthenticationToken(usuarioGoogle, null);
    }

    @Test
    void usuarioExistenteVoltaAoFrontComOToken() throws Exception {
        Usuario existente = new Usuario();
        when(usuarioRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(existente));
        when(jwtService.gerarToken(existente)).thenReturn("token.jwt.ana");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        sucesso.onAuthenticationSuccess(new MockHttpServletRequest(), resposta, loginGoogle("Ana Souza", "ana@exemplo.com"));

        assertThat(resposta.getRedirectedUrl()).isEqualTo(FRONT + "/oauth/callback?token=token.jwt.ana");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void primeiroLoginCriaUsuarioComumDoGoogle() throws Exception {
        when(usuarioRepository.findByEmail("bruno@exemplo.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("token.jwt.bruno");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        sucesso.onAuthenticationSuccess(new MockHttpServletRequest(), resposta, loginGoogle("Bruno Lima", "bruno@exemplo.com"));

        verify(usuarioRepository).save(argThat(novo ->
                novo.getEmail().equals("bruno@exemplo.com")
                        && novo.getNomeCompleto().equals("Bruno Lima")
                        && novo.getPerfil() == PerfilUsuario.USUARIO
                        && novo.getProvedor() == ProvedorAutenticacao.GOOGLE
                        && novo.getSenha() == null));
        assertThat(resposta.getRedirectedUrl()).isEqualTo(FRONT + "/oauth/callback?token=token.jwt.bruno");
    }

    @Test
    void falhaVoltaAoFrontSemToken() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        falha.onAuthenticationFailure(new MockHttpServletRequest(), resposta, new OAuth2AuthenticationException("access_denied"));

        assertThat(resposta.getRedirectedUrl()).isEqualTo(FRONT + "/oauth/callback");
    }
}
