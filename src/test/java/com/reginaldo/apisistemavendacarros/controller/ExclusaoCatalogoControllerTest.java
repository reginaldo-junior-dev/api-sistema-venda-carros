package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Marca, modelo, cor e categoria em uso não são excluídos, com mensagem que diz o motivo
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class ExclusaoCatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    private String token;
    private Carro carro;

    @BeforeEach
    void setUp() {
        token = fabrica.bearer(fabrica.usuario(PerfilUsuario.ADMINISTRADOR));
        carro = fabrica.carro(StatusCarro.DISPONIVEL);
    }

    @Test
    void marcaComModelosNaoEhExcluida() throws Exception {
        excluir("/marca/", carro.getModelo().getMarca().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Marca possui modelos cadastrados e não pode ser excluída"));
    }

    @Test
    void modeloComCarrosNaoEhExcluido() throws Exception {
        excluir("/modelo/", carro.getModelo().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Modelo possui carros cadastrados e não pode ser excluído"));
    }

    @Test
    void corComCarrosNaoEhExcluida() throws Exception {
        excluir("/cor/", carro.getCor().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cor possui carros cadastrados e não pode ser excluída"));
    }

    @Test
    void categoriaComCarrosNaoEhExcluida() throws Exception {
        excluir("/categoria/", carro.getCategoria().getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Categoria possui carros cadastrados e não pode ser excluída"));
    }

    private ResultActions excluir(String recurso, UUID id) throws Exception {
        return mockMvc.perform(delete(recurso + id).header("Authorization", token));
    }
}
