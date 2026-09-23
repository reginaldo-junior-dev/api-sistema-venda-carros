package com.reginaldo.apisistemavendacarros.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ClienteRequest(
        @NotBlank
        String cpf,

        @NotNull
        LocalDate dataNascimento,

        @NotBlank
        String telefone,

        @NotNull
        UUID usuarioId
) {
}
