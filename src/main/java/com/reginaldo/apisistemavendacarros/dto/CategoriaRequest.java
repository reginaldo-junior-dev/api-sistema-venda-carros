package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

public record CategoriaRequest(
        @NotNull
        String nome
) {
}
