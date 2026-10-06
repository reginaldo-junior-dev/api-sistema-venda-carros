package com.reginaldo.apisistemavendacarros;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Token CSRF do jeito que o site faz: visita a API, pega o cookie XSRF-TOKEN e devolve o valor no cabeçalho X-XSRF-TOKEN.
 * A visita é a GET /usuario/me sem sessão (401), a mesma primeira chamada do site, que não depende dos dados do teste.
 * Usado no lugar do csrf() do Spring Security Test, que troca o repositório do filtro CSRF compartilhado entre os testes:
 * depois dele, o token passava a ficar na sessão e os testes seguintes ficavam sem o cookie, conforme a ordem de execução.
 */
public final class CsrfReal {

    private CsrfReal() {
    }

    public static RequestPostProcessor tokenCsrf(MockMvc mockMvc) throws Exception {
        Cookie xsrf = mockMvc.perform(get("/usuario/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        if (xsrf == null) {
            throw new IllegalStateException("A API não devolveu o cookie XSRF-TOKEN");
        }

        return request -> {
            // Mantém os cookies que o teste já colocou (ex.: o da sessão)
            List<Cookie> cookies = new ArrayList<>(request.getCookies() == null ? List.of() : Arrays.asList(request.getCookies()));
            cookies.add(xsrf);
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", xsrf.getValue());
            return request;
        };
    }
}
