package com.reginaldo.apisistemavendacarros.exception;

import java.time.LocalDateTime;

public record ErroResposta(
        int status,
        String mensagem,
        LocalDateTime data
) {
}
