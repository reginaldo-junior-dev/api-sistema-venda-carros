package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.marca.MarcaRequest;
import com.reginaldo.apisistemavendacarros.dto.marca.MarcaResponse;
import com.reginaldo.apisistemavendacarros.entity.Marca;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MarcaMapper {
    Marca toEntity(MarcaRequest request);

    void atualizar(MarcaRequest request, @MappingTarget Marca marca);

    MarcaResponse toResponse(Marca marca);

}
