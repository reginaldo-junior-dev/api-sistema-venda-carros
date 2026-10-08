package com.reginaldo.apisistemavendacarros.dto.carro;

import com.reginaldo.apisistemavendacarros.enums.CondicaoCarro;
import com.reginaldo.apisistemavendacarros.enums.TipoCambio;
import com.reginaldo.apisistemavendacarros.enums.TipoCombustivel;
import jakarta.validation.constraints.Min;
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
        @Min(value = 1950, message = "Ano de fabricação inválido")
        Integer anoFabricacao,

        @NotNull
        @Min(value = 1950, message = "Ano do modelo inválido")
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
