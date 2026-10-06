package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sessão no navegador: token só no cookie HttpOnly e proteção CSRF para as requisições que usam o cookie
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = fabrica.usuario(PerfilUsuario.USUARIO);
    }

    private MockHttpServletResponse login() throws Exception {
        return mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(usuario.getEmail(), DadosTeste.SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void loginGravaOCookieHttpOnlyEDevolveAContaSemOToken() throws Exception {
        MockHttpServletResponse resposta = login();

        assertThat(resposta.getHeader("Set-Cookie"))
                .startsWith("sessao=")
                .contains("HttpOnly", "Secure", "SameSite=Lax", "Path=/", "Max-Age=3600");
        assertThat(resposta.getContentAsString())
                .contains(usuario.getEmail(), "\"perfil\":\"USUARIO\"")
                .doesNotContain("token");
    }

    @Test
    void cookieDaSessaoAutentica() throws Exception {
        Cookie sessao = login().getCookie("sessao");

        mockMvc.perform(get("/usuario/me").cookie(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));
    }

    @Test
    void comCookieAlterarDadosExigeOTokenCsrf() throws Exception {
        Cookie sessao = login().getCookie("sessao");
        String corpo = "{\"nomeCompleto\":\"Nome Novo\",\"email\":\"%s\"}".formatted(usuario.getEmail());

        // Outro site conseguiria fazer o navegador mandar o cookie, mas não o token CSRF
        mockMvc.perform(put("/usuario/me").cookie(sessao).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/usuario/me").cookie(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
    }

    @Test
    void loginSemTokenCsrfERecusado() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(usuario.getEmail(), DadosTeste.SENHA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void comCabecalhoBearerNaoPrecisaDeCsrf() throws Exception {
        // Outro site não consegue enviar o cabeçalho Authorization, então não há o que proteger
        mockMvc.perform(put("/usuario/me")
                        .header("Authorization", fabrica.bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Nome Novo\",\"email\":\"%s\"}".formatted(usuario.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    void logoutApagaOCookie() throws Exception {
        String setCookie = mockMvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getHeader("Set-Cookie");

        assertThat(setCookie).startsWith("sessao=;").contains("Max-Age=0", "HttpOnly");
    }

    @Test
    void cookieInvalidoNaoBloqueiaPaginaPublicaESeApaga() throws Exception {
        MockHttpServletResponse resposta = mockMvc.perform(get("/carro").cookie(new Cookie("sessao", "token.invalido")))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(resposta.getHeaders("Set-Cookie")).anySatisfy(c -> assertThat(c).startsWith("sessao=;").contains("Max-Age=0"));
    }

    @Test
    void diagnosticoCsrf() throws Exception {
        org.springframework.test.web.servlet.MvcResult r = mockMvc.perform(get("/carro")).andReturn();
        jakarta.servlet.http.HttpServletRequest req = r.getRequest();
        Object token = req.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
        String valor = token == null ? "SEM ATRIBUTO" : ((org.springframework.security.web.csrf.CsrfToken) token).getToken();
        java.util.List<String> atributos = java.util.Collections.list(req.getAttributeNames());
        throw new AssertionError("DIAG token=" + (valor == null ? "null" : valor.length() + "chars")
                + " | tipo=" + (token == null ? "-" : token.getClass().getSimpleName())
                + " | atributos=" + atributos
                + " | headers=" + r.getResponse().getHeaderNames()
                + " | setcookie=" + r.getResponse().getHeaders("Set-Cookie")
                + " | cookies=" + java.util.Arrays.toString(r.getResponse().getCookies())
                + " | java=" + System.getProperty("java.version"));
    }

    @Test
    void primeiraVisitaJaRecebeOCookieCsrf() throws Exception {
        // Sem isso, o primeiro POST do site (login, cadastro) seria recusado por falta do token
        MockHttpServletResponse resposta = mockMvc.perform(get("/carro")).andReturn().getResponse();

        // A mensagem mostra o que a API respondeu, para a falha (se houver) explicar a si mesma no CI
        assertThat(resposta.getHeaders("Set-Cookie"))
                .as("status %d, cookies recebidos: %s", resposta.getStatus(), resposta.getHeaders("Set-Cookie"))
                .anySatisfy(cookie -> assertThat(cookie).startsWith("XSRF-TOKEN="));
    }
}
