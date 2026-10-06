package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class ExclusaoClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private InteresseCarroRepository interesseCarroRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;
    private Usuario administrador;
    private Cliente cliente;
    private Carro carro;

    @BeforeEach
    void setUp() {
        usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
        cliente = fabrica.cliente(usuario);
        carro = fabrica.carro(StatusCarro.DISPONIVEL);
    }

    @Test
    void excluirMeApagaFavoritosInteressesEEnderecos() throws Exception {
        fabrica.favorito(cliente, carro);
        fabrica.interesse(cliente, carro);
        fabrica.endereco(cliente);

        mockMvc.perform(delete("/cliente/me").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).isEmpty();
        assertThat(interesseCarroRepository.findByClienteId(cliente.getId())).isEmpty();
        assertThat(enderecoRepository.findAllByClienteId(cliente.getId())).isEmpty();
    }

    @Test
    void administradorExcluiClienteComFavoritosEInteresses() throws Exception {
        fabrica.favorito(cliente, carro);
        fabrica.interesse(cliente, carro);

        mockMvc.perform(delete("/cliente/" + cliente.getId()).header("Authorization", fabrica.bearer(administrador)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
    }

    @Test
    void clienteComCompraNaoPodeSerExcluido() throws Exception {
        Compra compra = fabrica.compra(cliente, fabrica.carro(StatusCarro.DISPONIVEL), StatusCompra.CANCELADA);
        fabrica.favorito(cliente, carro);

        mockMvc.perform(delete("/cliente/me").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Cliente possui compras registradas e não pode ser excluído"));

        mockMvc.perform(delete("/cliente/" + cliente.getId()).header("Authorization", fabrica.bearer(administrador)))
                .andExpect(status().isBadRequest());

        assertThat(clienteRepository.existsById(cliente.getId())).isTrue();
        assertThat(compraRepository.existsById(compra.getId())).isTrue();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).hasSize(1);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }
}
