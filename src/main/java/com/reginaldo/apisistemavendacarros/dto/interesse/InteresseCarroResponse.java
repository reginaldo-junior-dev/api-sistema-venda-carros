package com.reginaldo.apisistemavendacarros.dto.interesse;

import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteresseCarroResponse(
        UUID id,
        UUID carroId,
        String nome,
        String email,
        String telefone,
        String mensagem,
        StatusInteresse status,
        LocalDateTime dataInteresse,
        UUID clienteId
) {
}
