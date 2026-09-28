package com.reginaldo.apisistemavendacarros.dto;

import java.time.LocalDateTime;

public record ErroResposta(
        int status,
        String mensagem,
        LocalDateTime data
) {
}
