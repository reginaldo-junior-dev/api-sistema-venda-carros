package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.Cor;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.CorRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CorRepository corRepository;

    @Autowired
    private JwtService jwtService;

    private Usuario usuario;
    private Usuario administrador;
    private Cor cor;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);

        cor = new Cor();
        cor.setNome("Cor " + UUID.randomUUID().toString().substring(0, 8));
        corRepository.saveAndFlush(cor);
    }

    @Test
    void consultaDeCoresEPublica() throws Exception {
        mockMvc.perform(get("/cor")).andExpect(status().isOk());

        mockMvc.perform(get("/cor/" + cor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(cor.getNome()));
    }

    @Test
    void usuarioComumNaoCriaAlteraNemExcluiCor() throws Exception {
        String token = bearer(usuario);

        mockMvc.perform(post("/cor").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Roxo\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/cor/" + cor.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Roxo\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/cor/" + cor.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());

        assertThat(corRepository.findById(cor.getId())).isPresent();
    }

    @Test
    void semAutenticacaoNaoCriaCor() throws Exception {
        mockMvc.perform(post("/cor").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Roxo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorCriaAlteraEExcluiCor() throws Exception {
        String token = bearer(administrador);
        String nome = "Roxo " + UUID.randomUUID().toString().substring(0, 8);

        mockMvc.perform(post("/cor").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"%s\"}".formatted(nome)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(nome));

        mockMvc.perform(put("/cor/" + cor.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"%s\"}".formatted(nome + " 2")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/cor/" + cor.getId()).header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private Usuario criarUsuario(PerfilUsuario perfil) {
        Usuario novo = new Usuario();
        novo.setNomeCompleto("Teste " + perfil);
        novo.setEmail(UUID.randomUUID() + "@teste.com");
        novo.setPerfil(perfil);
        novo.setProvedor(ProvedorAutenticacao.LOCAL);
        return usuarioRepository.saveAndFlush(novo);
    }
}
