package com.reginaldo.apisistemavendacarros.dto.categoria;

import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome
) {
}
