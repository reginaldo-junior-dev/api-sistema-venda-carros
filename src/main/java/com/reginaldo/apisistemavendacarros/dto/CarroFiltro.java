package com.reginaldo.apisistemavendacarros.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CarroFiltro(
        String nome,
        UUID categoriaId,
        UUID marcaId,
        UUID modeloId,
        UUID corId,
        String condicao,
        String combustivel,
        String cambio,
        String status,
        BigDecimal precoMin,
        BigDecimal precoMax,
        Integer anoMin,
        Integer anoMax,
        Integer quilometragemMax

) {
}
