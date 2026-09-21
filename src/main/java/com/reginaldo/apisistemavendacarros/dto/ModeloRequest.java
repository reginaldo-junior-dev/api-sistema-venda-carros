package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

public record ModeloRequest(
        @NotNull
        String nome
) {
}
