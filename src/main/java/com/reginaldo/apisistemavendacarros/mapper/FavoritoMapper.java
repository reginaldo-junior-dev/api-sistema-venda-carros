package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Favorito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoritoMapper {
    @Mapping(source = "carro.id", target = "carroId")
    FavoritoResponse toResponse(Favorito favorito);
}
