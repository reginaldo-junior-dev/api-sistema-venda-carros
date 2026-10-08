package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.repository.ImagemCarroRepository;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: o arquivo do S3 só é removido depois de um commit real
@SpringBootTest
@AutoConfigureMockMvc
@Import(DadosTeste.class)
class ExclusaoImagemTransacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private ImagemCarroRepository imagemCarroRepository;

    @MockitoBean
    private S3Service s3Service;

    private Usuario administrador;
    private Carro carro;

    @BeforeEach
    void setUp() {
        administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
        carro = fabrica.carro(StatusCarro.DISPONIVEL);
    }

    @AfterEach
    void limpar() {
        fabrica.limpar();
    }

    @Test
    void excluirFotoPrincipalPromoveAProximaEApagaOArquivoDepoisDoCommit() throws Exception {
        ImagemCarro principal = fabrica.imagem(carro, "carros/foto-1.jpg");
        // A fábrica cria a foto como principal, e o banco aceita só uma principal por carro
        ImagemCarro segunda = new ImagemCarro();
        segunda.setCarro(carro);
        segunda.setChaveArquivo("carros/foto-2.jpg");
        segunda.setOrdem(2);
        segunda.setPrincipal(false);
        imagemCarroRepository.saveAndFlush(segunda);

        mockMvc.perform(delete("/carro/imagens/" + principal.getId()).header("Authorization", fabrica.bearer(administrador)))
                .andExpect(status().isNoContent());

        assertThat(imagemCarroRepository.existsById(principal.getId())).isFalse();
        assertThat(imagemCarroRepository.findById(segunda.getId()).orElseThrow().getPrincipal()).isTrue();
        verify(s3Service).excluir("carros/foto-1.jpg");
    }
}
