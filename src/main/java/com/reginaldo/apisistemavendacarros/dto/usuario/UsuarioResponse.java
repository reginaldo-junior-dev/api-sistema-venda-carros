package com.reginaldo.apisistemavendacarros.dto.usuario;

import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nomeCompleto,
        String email,
        String provedor,
        PerfilUsuario perfil
) {
}
