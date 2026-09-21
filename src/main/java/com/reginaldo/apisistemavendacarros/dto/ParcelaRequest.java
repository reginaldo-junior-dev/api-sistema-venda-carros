package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ParcelaRequest(
        @NotNull
        UUID pagamentoId
) {
}
