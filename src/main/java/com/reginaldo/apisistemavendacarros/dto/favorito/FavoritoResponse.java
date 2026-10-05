package com.reginaldo.apisistemavendacarros.dto.favorito;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoritoResponse(
       UUID id,
       UUID carroId,
       LocalDateTime dataFavorito
) {
}
