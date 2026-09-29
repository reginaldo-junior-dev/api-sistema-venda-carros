package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;
import jakarta.validation.constraints.NotNull;

public record InteresseCarroStatusRequest(
        @NotNull(message = "Status é obrigatório")
        StatusInteresse status
) {
}
