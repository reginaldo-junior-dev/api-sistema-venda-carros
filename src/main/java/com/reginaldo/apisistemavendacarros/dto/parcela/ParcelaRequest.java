package com.reginaldo.apisistemavendacarros.dto.parcela;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ParcelaRequest(
        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade deve ser de no mínimo 1 parcela")
        @Max(value = 12, message = "Quantidade deve ser de no máximo 12 parcelas")
        Integer quantidade
) {
}
