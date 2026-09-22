package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.MarcaRequest;
import com.reginaldo.apisistemavendacarros.dto.MarcaResponse;
import com.reginaldo.apisistemavendacarros.entity.Marca;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MarcaMapper {
    Marca toEntity(MarcaRequest request);

    void atualizar(MarcaRequest request, @MappingTarget Marca marca);

    MarcaResponse toResponse(Marca marca);

}
