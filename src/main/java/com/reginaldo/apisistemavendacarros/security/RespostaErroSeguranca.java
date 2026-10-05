package com.reginaldo.apisistemavendacarros.security;

import com.reginaldo.apisistemavendacarros.dto.erro.ErroResposta;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

// 401 e 403 acontecem nos filtros do Spring Security, antes do @RestControllerAdvice.
// Por isso o ErroResposta é montado aqui
@Component
@RequiredArgsConstructor
public class RespostaErroSeguranca {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint naoAutenticado() {
        return (request, response, ex) -> escrever(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária");
    }

    public AccessDeniedHandler acessoNegado() {
        return (request, response, ex) -> escrever(response, HttpStatus.FORBIDDEN, "Acesso negado");
    }

    public void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        ErroResposta erroResposta = new ErroResposta(status.value(), mensagem, LocalDateTime.now());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(erroResposta));
    }
}
