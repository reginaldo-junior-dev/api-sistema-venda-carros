package com.reginaldo.apisistemavendacarros.dto.compra;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompraRequest(
        @NotNull(message = "Carro é obrigatório")
        UUID carroId
) {
}
