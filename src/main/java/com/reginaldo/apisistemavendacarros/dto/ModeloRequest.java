package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ModeloRequest(
        @NotBlank
        String nome,

        @NotNull
        UUID marcaId
) {
}
