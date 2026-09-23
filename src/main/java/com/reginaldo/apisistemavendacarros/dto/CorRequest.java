package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;

public record CorRequest(
        @NotBlank
        String nome
) {
}
