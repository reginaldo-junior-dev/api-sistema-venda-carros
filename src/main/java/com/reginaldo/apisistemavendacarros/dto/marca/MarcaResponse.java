package com.reginaldo.apisistemavendacarros.dto.marca;

import java.util.UUID;

public record MarcaResponse(
        UUID id,
        String nome
) {
}
