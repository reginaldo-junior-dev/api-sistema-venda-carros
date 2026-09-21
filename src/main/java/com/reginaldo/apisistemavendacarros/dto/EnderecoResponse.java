package com.reginaldo.apisistemavendacarros.dto;

import java.util.UUID;

public record EnderecoResponse(
        UUID id,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        Boolean principal,
        UUID clienteId
) {
}
