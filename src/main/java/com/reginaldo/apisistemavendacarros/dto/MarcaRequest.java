package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

public record MarcaRequest(
        @NotNull
        String nome
) {
}
