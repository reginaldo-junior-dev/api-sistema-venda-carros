package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.MetodoPagamento;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PagamentoRequest(
        @NotNull
        MetodoPagamento metodo,

        @NotNull
        UUID compraId
) {
}
