package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;

import java.time.LocalDateTime;
import java.util.UUID;

public record InteresseCarroResponse(
        UUID id,
        String nome,
        String email,
        String telefone,
        String mensagem,
        StatusInteresse status,
        LocalDateTime dataInteresse,
        UUID carroId,
        UUID clienteId
) {
}
