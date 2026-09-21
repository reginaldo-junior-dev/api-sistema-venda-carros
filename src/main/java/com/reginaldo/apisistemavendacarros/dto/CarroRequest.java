package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.CondicaoCarro;
import com.reginaldo.apisistemavendacarros.enums.TipoCambio;
import com.reginaldo.apisistemavendacarros.enums.TipoCombustivel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CarroRequest(
        @NotBlank
        String nome,

        @NotNull
        @Positive
        BigDecimal preco,

        @NotBlank
        String descricao,

        @NotNull
        @Positive
        Integer anoFabricacao,

        @NotNull
        @Positive
        Integer anoModelo,

        @NotNull
        @PositiveOrZero
        Integer quilometragem,

        @NotNull
        CondicaoCarro condicao,

        @NotNull
        TipoCombustivel combustivel,

        @NotNull
        TipoCambio cambio,

        @NotNull
        UUID modeloId,

        @NotNull
        UUID categoriaId,

        @NotNull
        UUID corId

) {
}
