package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarcaRequest(
        @NotNull
        String nome,

        @NotNull
        UUID marcaId
) {
}
