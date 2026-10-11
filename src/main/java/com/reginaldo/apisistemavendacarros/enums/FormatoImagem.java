package com.reginaldo.apisistemavendacarros.enums;

import java.util.Arrays;
import java.util.Optional;

// Formatos aceitos nas fotos dos carros, reconhecidos pelos primeiros bytes do arquivo.
// O tipo informado pelo navegador e a extensão do nome são escolhidos por quem envia e não provam nada
public enum FormatoImagem {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    // Bytes necessários para reconhecer qualquer um dos formatos (o WebP é o mais longo)
    public static final int TAMANHO_ASSINATURA = 12;

    private final String contentType;
    private final String extensao;

    FormatoImagem(String contentType, String extensao) {
        this.contentType = contentType;
        this.extensao = extensao;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtensao() {
        return extensao;
    }

    public static Optional<FormatoImagem> detectar(byte[] inicio) {
        return Arrays.stream(values()).filter(formato -> formato.reconhece(inicio)).findFirst();
    }

    private boolean reconhece(byte[] b) {
        return switch (this) {
            case JPEG -> comeca(b, 0, 0xFF, 0xD8, 0xFF);
            case PNG -> comeca(b, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            // RIFF <tamanho em 4 bytes> WEBP
            case WEBP -> comeca(b, 0, 'R', 'I', 'F', 'F') && comeca(b, 8, 'W', 'E', 'B', 'P');
        };
    }

    private static boolean comeca(byte[] b, int posicao, int... esperado) {
        if (b.length < posicao + esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if ((b[posicao + i] & 0xFF) != esperado[i]) {
                return false;
            }
        }
        return true;
    }
}
