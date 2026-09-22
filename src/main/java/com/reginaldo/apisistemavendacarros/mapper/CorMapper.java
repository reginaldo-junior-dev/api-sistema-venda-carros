package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.CorResponse;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CorMapper {
    Cor toEntity(CorRequest request);

    void atualizar(CorRequest request, @MappingTarget Cor cor);

    CorResponse toResponse(Cor cor);
}
