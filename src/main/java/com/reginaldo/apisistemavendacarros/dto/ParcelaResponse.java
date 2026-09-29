package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.StatusParcela;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ParcelaResponse(
        UUID id,
        UUID pagamentoId,
        Integer numero,
        BigDecimal valor,
        StatusParcela status,
        LocalDate dataVencimento,
        LocalDate dataPagamento
) {
}
