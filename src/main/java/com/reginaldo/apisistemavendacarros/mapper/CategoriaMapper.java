package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CategoriaRequest;
import com.reginaldo.apisistemavendacarros.dto.CategoriaResponse;
import com.reginaldo.apisistemavendacarros.entity.Categoria;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    Categoria toEntity(CategoriaRequest request);
    CategoriaResponse toResponse(Categoria categoria);
}
