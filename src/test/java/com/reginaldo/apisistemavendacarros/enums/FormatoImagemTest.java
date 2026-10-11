package com.reginaldo.apisistemavendacarros.enums;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoImagemTest {

    private static byte[] bytes(int... valores) {
        byte[] b = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) {
            b[i] = (byte) valores[i];
        }
        return b;
    }

    @Test
    void reconheceOsTresFormatosPelosPrimeirosBytes() {
        assertThat(FormatoImagem.detectar(bytes(0xFF, 0xD8, 0xFF, 0xE0))).contains(FormatoImagem.JPEG);
        assertThat(FormatoImagem.detectar(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A))).contains(FormatoImagem.PNG);
        assertThat(FormatoImagem.detectar(bytes('R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'))).contains(FormatoImagem.WEBP);
    }

    @Test
    void recusaOutrosArquivosEArquivoCurto() {
        assertThat(FormatoImagem.detectar("<svg xmlns=\"http://www.w3.org/2000/svg\">".getBytes(StandardCharsets.UTF_8))).isEmpty();
        // RIFF de áudio (WAVE), não de imagem
        assertThat(FormatoImagem.detectar(bytes('R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'A', 'V', 'E'))).isEmpty();
        assertThat(FormatoImagem.detectar(bytes(0xFF, 0xD8))).isEmpty();
        assertThat(FormatoImagem.detectar(new byte[0])).isEmpty();
    }
}
