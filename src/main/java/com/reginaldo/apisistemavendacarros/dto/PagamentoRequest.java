package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.MetodoPagamento;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PagamentoRequest(
        @NotNull(message = "Compra é obrigatória")
        UUID compraId,

        @NotNull(message = "Método de pagamento é obrigatório")
        MetodoPagamento metodo
) {
}
