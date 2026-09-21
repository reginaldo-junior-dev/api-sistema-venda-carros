package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.StatusCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        BigDecimal valorTotal,
        LocalDateTime dataCompra,
        StatusCompra status,
        UUID carroId,
        UUID clienteId
) {
}
