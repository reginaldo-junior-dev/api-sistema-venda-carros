package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CompraRequest(
        @NotNull
        UUID clienteId,

        @NotNull
        UUID carroId
) {
}
