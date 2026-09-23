package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
        @NotBlank
        String nome
) {
}
