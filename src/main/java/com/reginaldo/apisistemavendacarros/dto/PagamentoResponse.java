package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.MetodoPagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoResponse(
        UUID id,
        BigDecimal valor,
        MetodoPagamento metodo,
        StatusPagamento status,
        LocalDateTime dataPagamento,
        String idExterno,
        UUID compraId
) {
}
