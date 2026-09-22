package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CategoriaRequest;
import com.reginaldo.apisistemavendacarros.dto.CategoriaResponse;
import com.reginaldo.apisistemavendacarros.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    Categoria toEntity(CategoriaRequest request);

    void atualizar(CategoriaRequest request, @MappingTarget Categoria categoria);
    CategoriaResponse toResponse(Categoria categoria);
}
