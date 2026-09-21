package com.reginaldo.apisistemavendacarros.dto;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nomeCompleto,
        String email,
        String provedor,
        String role
) {
}
