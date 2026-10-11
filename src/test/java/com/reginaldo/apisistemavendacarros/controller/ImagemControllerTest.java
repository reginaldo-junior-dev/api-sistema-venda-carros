package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import com.reginaldo.apisistemavendacarros.enums.FormatoImagem;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.repository.ImagemCarroRepository;
import com.reginaldo.apisistemavendacarros.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class ImagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private ImagemCarroRepository imagemCarroRepository;

    @MockitoBean
    private S3Service s3Service;

    private String token;

    @BeforeEach
    void setUp() {
        token = fabrica.bearer(fabrica.usuario(PerfilUsuario.ADMINISTRADOR));
    }

    @Test
    void carroVendidoNaoRecebeFotoNova() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.VENDIDO);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/carro/" + carro.getId() + "/imagens").file(arquivo).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Fotos de carro vendido não podem ser alteradas"));

        verify(s3Service, never()).upload(any(), any());
    }

    @Test
    void arquivoQueNaoEhImagemERecusadoMesmoComNomeETipoDeFoto() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", html);

        mockMvc.perform(multipart("/carro/" + carro.getId() + "/imagens").file(arquivo).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A foto precisa ser uma imagem JPEG, PNG ou WebP"));

        verify(s3Service, never()).upload(any(), any());
    }

    @Test
    void formatoVemDosBytesENaoDoQueONavegadorInforma() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "pagina.html", "text/html", png);
        when(s3Service.upload(any(), eq(FormatoImagem.PNG))).thenReturn("chave.png");

        mockMvc.perform(multipart("/carro/" + carro.getId() + "/imagens").file(arquivo).header("Authorization", token))
                .andExpect(status().isOk());

        verify(s3Service).upload(any(), eq(FormatoImagem.PNG));
    }

    @Test
    void fotoDeCarroVendidoNaoEhExcluida() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.VENDIDO);
        ImagemCarro imagem = fabrica.imagem(carro, "carros/vendido.jpg");

        mockMvc.perform(delete("/carro/imagens/" + imagem.getId()).header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Fotos de carro vendido não podem ser alteradas"));

        verify(s3Service, never()).excluir(any());
        assertThat(imagemCarroRepository.existsById(imagem.getId())).isTrue();
    }
}
