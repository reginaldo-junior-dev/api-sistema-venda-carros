package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.StatusParcela;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ParcelaResponse(
        UUID id,
        Integer numero,
        BigDecimal valor,
        StatusParcela status,
        LocalDateTime dataPagamento,
        LocalDate dataVencimento,
        UUID pagamentoId
) {
}
