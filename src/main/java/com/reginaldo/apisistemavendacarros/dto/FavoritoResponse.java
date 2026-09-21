package com.reginaldo.apisistemavendacarros.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoritoResponse(
       UUID id,
       LocalDateTime dataFavorito,
       UUID carroId,
       UUID clienteId
) {
}
