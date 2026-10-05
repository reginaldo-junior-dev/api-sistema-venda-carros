package com.reginaldo.apisistemavendacarros.dto.cliente;

import java.time.LocalDate;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
        String cpf,
        LocalDate dataNascimento,
        String telefone,
        UUID usuarioId
) {
}
