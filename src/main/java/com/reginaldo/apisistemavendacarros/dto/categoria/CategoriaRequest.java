package com.reginaldo.apisistemavendacarros.dto.categoria;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
        @NotBlank
        String nome
) {
}
