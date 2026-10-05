package com.reginaldo.apisistemavendacarros.dto.cor;

import jakarta.validation.constraints.NotBlank;

public record CorRequest(
        @NotBlank
        String nome
) {
}
