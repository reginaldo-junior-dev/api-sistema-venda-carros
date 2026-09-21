package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record FavoritoRequest(

        @NotNull
        UUID carroId,

        @NotNull
        UUID clienteId
) {
}
