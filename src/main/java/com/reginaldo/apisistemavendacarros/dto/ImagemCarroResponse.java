package com.reginaldo.apisistemavendacarros.dto;

import java.util.UUID;

public record ImagemCarroResponse(
        UUID id,
        String url,
        Integer ordem,
        Boolean principal,
        UUID carroId

) {
}
