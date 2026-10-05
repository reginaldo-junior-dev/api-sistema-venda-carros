package com.reginaldo.apisistemavendacarros.dto.imagem;

import java.util.UUID;

public record ImagemCarroResponse(
        UUID id,
        String chaveArquivo,
        String url,
        Integer ordem,
        Boolean principal,
        UUID carroId

) {
}
