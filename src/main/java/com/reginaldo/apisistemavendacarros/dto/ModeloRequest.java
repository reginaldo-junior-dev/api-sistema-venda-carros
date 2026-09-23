package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ModeloRequest(
        @NotNull
        String nome,

        @NotNull
        UUID marcaId
) {
}
