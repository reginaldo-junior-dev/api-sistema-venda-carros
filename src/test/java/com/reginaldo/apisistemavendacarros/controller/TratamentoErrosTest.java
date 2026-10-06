package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Todos os erros saem no formato ErroResposta, inclusive os dos filtros do Spring Security
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class TratamentoErrosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Test
    void semTokenRetorna401ComCorpo() throws Exception {
        mockMvc.perform(get("/cliente/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void tokenInvalidoRetorna401ComCorpo() throws Exception {
        mockMvc.perform(get("/cliente/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Token inválido ou expirado"));
    }

    @Test
    void perfilSemPermissaoRetorna403ComCorpo() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);

        mockMvc.perform(get("/compra").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));
    }

    @Test
    void acessoNegadoPeloServiceRetorna403ComCorpo() throws Exception {
        // AccessDeniedException lançada no PagamentoService (compra de outro cliente)
        Cliente dono = fabrica.cliente(fabrica.usuario(PerfilUsuario.USUARIO));
        Compra compra = fabrica.compra(dono, fabrica.carro(StatusCarro.RESERVADO), StatusCompra.PENDENTE);
        Usuario intruso = fabrica.usuario(PerfilUsuario.USUARIO);
        fabrica.cliente(intruso);

        mockMvc.perform(post("/pagamento")
                        .header("Authorization", fabrica.bearer(intruso))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compraId\":\"%s\",\"metodo\":\"PIX\"}".formatted(compra.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));
    }

    @Test
    void enumInexistenteRetorna400NoFormatoDoProjeto() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);

        mockMvc.perform(post("/pagamento")
                        .header("Authorization", fabrica.bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compraId\":\"00000000-0000-0000-0000-000000000000\",\"metodo\":\"CHEQUE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido ou com valor não reconhecido"));
    }

    @Test
    void jsonMalformadoRetorna400NoFormatoDoProjeto() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);

        mockMvc.perform(post("/compra")
                        .header("Authorization", fabrica.bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carroId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").exists());
    }
}
