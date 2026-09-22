package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ModeloRequest;
import com.reginaldo.apisistemavendacarros.dto.ModeloResponse;
import com.reginaldo.apisistemavendacarros.entity.Modelo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ModeloMapper {
    Modelo toEntity(ModeloRequest request);

    void atualizar(ModeloRequest request, @MappingTarget Modelo modelo);

    ModeloResponse toResponse(Modelo modelo);

}
