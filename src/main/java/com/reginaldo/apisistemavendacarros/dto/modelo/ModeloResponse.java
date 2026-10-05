package com.reginaldo.apisistemavendacarros.dto.modelo;

import java.util.UUID;

public record ModeloResponse(
        UUID id,
        String nome,
        UUID marcaId
) {
}
