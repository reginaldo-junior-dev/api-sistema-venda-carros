package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest (
        @NotBlank
        String nomeCompleto,

        @NotBlank
        String email,

        @NotBlank
        String senha
) {
}
