package com.reginaldo.apisistemavendacarros.dto.compra;

import com.reginaldo.apisistemavendacarros.enums.StatusCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        UUID carroId,
        UUID clienteId,
        BigDecimal valorTotal,
        StatusCompra status,
        LocalDateTime dataCompra
) {
}
