package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: as imagens do S3 só são removidas depois de um commit real
@SpringBootTest
@AutoConfigureMockMvc
@Import(DadosTeste.class)
class ExclusaoCarroTransacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private InteresseCarroRepository interesseCarroRepository;

    @MockitoBean
    private S3Service s3Service;

    private Usuario administrador;
    private Cliente cliente;
    private Carro carro;

    @BeforeEach
    void setUp() {
        administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
        cliente = fabrica.cliente(fabrica.usuario(PerfilUsuario.USUARIO));
        carro = fabrica.carro(StatusCarro.DISPONIVEL);
        fabrica.imagem(carro, "carros/foto-1.jpg");
    }

    @AfterEach
    void limpar() {
        fabrica.limpar();
    }

    @Test
    void excluirCarroSemCompraApagaFavoritosInteressesEArquivosDepoisDoCommit() throws Exception {
        fabrica.favorito(cliente, carro);
        fabrica.interesse(cliente, carro);

        mockMvc.perform(delete("/carro/" + carro.getId()).header("Authorization", fabrica.bearer(administrador)))
                .andExpect(status().isNoContent());

        assertThat(carroRepository.existsById(carro.getId())).isFalse();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).isEmpty();
        assertThat(interesseCarroRepository.findByClienteId(cliente.getId())).isEmpty();
        verify(s3Service).excluir("carros/foto-1.jpg");
    }

    @Test
    void carroComCompraNaoPodeSerExcluidoENenhumaImagemEhApagada() throws Exception {
        fabrica.compra(cliente, carro, StatusCompra.CANCELADA);
        fabrica.favorito(cliente, carro);

        mockMvc.perform(delete("/carro/" + carro.getId()).header("Authorization", fabrica.bearer(administrador)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro possui compras registradas e não pode ser excluído"));

        assertThat(carroRepository.existsById(carro.getId())).isTrue();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).hasSize(1);
        verify(s3Service, never()).excluir(anyString());
    }
}
