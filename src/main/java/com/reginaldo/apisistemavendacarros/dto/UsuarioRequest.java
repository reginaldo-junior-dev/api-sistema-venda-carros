package com.reginaldo.apisistemavendacarros.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequest (
        @NotBlank
        String nomeCompleto,

        @NotBlank
        String email,

        @NotBlank
        String senha
) {
}
