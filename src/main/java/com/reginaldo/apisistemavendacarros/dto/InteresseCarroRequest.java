package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InteresseCarroRequest(
        @NotBlank
        String nome,

        @NotBlank
        String email,

        @NotBlank
        String telefone,

        @NotBlank
        String mensagem,

        @NotNull
        UUID carroId,

        @NotNull
        UUID clienteId

) {
}
