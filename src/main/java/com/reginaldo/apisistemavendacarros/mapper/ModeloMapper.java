package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ModeloRequest;
import com.reginaldo.apisistemavendacarros.dto.ModeloResponse;
import com.reginaldo.apisistemavendacarros.entity.Modelo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ModeloMapper {
    Modelo toEntity(ModeloRequest request);
    ModeloResponse toResponse(Modelo modelo);

}
