package com.reginaldo.apisistemavendacarros.dto;

import java.util.UUID;

public record MarcaResponse(
        UUID id,
        String nome
) {
}
