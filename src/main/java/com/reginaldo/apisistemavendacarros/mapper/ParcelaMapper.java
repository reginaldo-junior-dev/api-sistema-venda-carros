package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Parcela;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParcelaMapper {
    @Mapping(source = "pagamento.id", target = "pagamentoId")
    ParcelaResponse toResponse(Parcela parcela);
}
