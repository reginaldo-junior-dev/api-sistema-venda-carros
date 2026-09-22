package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ParcelaRequest;
import com.reginaldo.apisistemavendacarros.dto.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Parcela;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ParcelaMapper {
    Parcela toEntity(ParcelaRequest request);

    void atualizar(ParcelaRequest request, @MappingTarget Parcela parcela);

    @Mapping(source = "pagamento.id", target = "pagamentoId")
    ParcelaResponse toResponse(Parcela parcela);
}
