package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class CarroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario administrador;

    @BeforeEach
    void setUp() {
        administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
    }

    @Test
    void administradorAtualizaCarroDisponivel() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);

        atualizar(carro, "Carro editado", 2020, 2021)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carro editado"));
    }

    @Test
    void carroVendidoNaoPodeSerAlterado() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.VENDIDO);
        String nomeOriginal = carro.getNome();

        atualizar(carro, "Carro editado", 2020, 2021)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro vendido não pode ser alterado"));

        assertThat(carroRepository.findById(carro.getId()).orElseThrow().getNome()).isEqualTo(nomeOriginal);
    }

    @ParameterizedTest
    @ValueSource(ints = {2019, 2022})
    void anoDoModeloForaDaFaixaRetorna400(int anoModelo) throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);

        atualizar(carro, "Carro editado", 2020, anoModelo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.anoModelo")
                        .value("Ano do modelo deve ser igual ao ano de fabricação ou o seguinte"));
    }

    @Test
    void anoDeFabricacaoNoFuturoRetorna400() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);
        int anoQueVem = Year.now().getValue() + 1;

        atualizar(carro, "Carro editado", anoQueVem, anoQueVem)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.anoFabricacao").value("Ano de fabricação não pode estar no futuro"));
    }

    private ResultActions atualizar(Carro carro, String nome, int anoFabricacao, int anoModelo) throws Exception {
        String body = """
                {"nome":"%s","preco":60000.00,"descricao":"Descrição","anoFabricacao":%d,"anoModelo":%d,
                 "quilometragem":15000,"condicao":"USADO","combustivel":"FLEX","cambio":"MANUAL",
                 "modeloId":"%s","categoriaId":"%s","corId":"%s"}
                """.formatted(nome, anoFabricacao, anoModelo, carro.getModelo().getId(), carro.getCategoria().getId(), carro.getCor().getId());

        // A requisição carrega o carro do banco, como em produção, e não a instância criada pela fábrica
        entityManager.clear();

        return mockMvc.perform(put("/carro/" + carro.getId())
                .header("Authorization", fabrica.bearer(administrador))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
