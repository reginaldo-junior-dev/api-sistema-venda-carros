package com.reginaldo.apisistemavendacarros.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErroValidacao(
        int status,
        Map<String, String> mensagens,
        LocalDateTime data
) {
}
