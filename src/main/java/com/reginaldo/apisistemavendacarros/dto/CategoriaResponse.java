package com.reginaldo.apisistemavendacarros.dto;

import java.util.UUID;

public record CategoriaResponse(
        UUID id,
        String nome
) {
}
