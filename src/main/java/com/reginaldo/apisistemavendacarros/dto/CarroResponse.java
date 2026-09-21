package com.reginaldo.apisistemavendacarros.dto;

import com.reginaldo.apisistemavendacarros.enums.CondicaoCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.TipoCambio;
import com.reginaldo.apisistemavendacarros.enums.TipoCombustivel;


import java.math.BigDecimal;
import java.util.UUID;

public record CarroResponse (
        UUID id,
        String nome,
        BigDecimal preco,
        String descricao,
        Integer anoFabricacao,
        Integer anoModelo,
        Integer quilometragem,
        CondicaoCarro condicao,
        TipoCombustivel combustivel,
        TipoCambio cambio,
        StatusCarro status,
        UUID modeloId,
        UUID categoriaId,
        UUID corId

) {

}
