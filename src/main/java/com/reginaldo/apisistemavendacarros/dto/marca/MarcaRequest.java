package com.reginaldo.apisistemavendacarros.dto.marca;

import jakarta.validation.constraints.NotBlank;

public record MarcaRequest(
        @NotBlank
        String nome
) {
}
