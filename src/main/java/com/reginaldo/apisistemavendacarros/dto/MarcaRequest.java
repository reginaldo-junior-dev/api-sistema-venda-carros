package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;

public record MarcaRequest(
        @NotBlank
        String nome
) {
}
