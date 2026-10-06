package com.reginaldo.apisistemavendacarros.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentacaoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentacaoEPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void gruposSemDuplicadosEWebhookEscondido() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.tags", hasSize(15)))
                .andExpect(jsonPath("$.tags[0].name").value("Autenticação"))
                .andExpect(jsonPath("$.paths['/stripe/webhook']").doesNotExist());
    }

    @Test
    void jwtNoBotaoAuthorizeERotasPublicasSemCadeado() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/carro'].get.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/compra'].post.security").doesNotExist());
    }

    @Test
    void uploadDeImagemComSeletorDeArquivo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/carro/{id}/imagens'].post.requestBody.content['multipart/form-data']"
                        + ".schema.properties.arquivo.format").value("binary"));
    }

    @Test
    void parametrosDeListagemExpandidos() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/cliente/me/compras'].get.parameters[*].name",
                        containsInAnyOrder("page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/carro'].get.parameters[*].name", hasItems("precoMin", "page")));
    }
}
