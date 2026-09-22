package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.FavoritoRequest;
import com.reginaldo.apisistemavendacarros.dto.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Favorito;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoritoMapper {
    Favorito toEntity(FavoritoRequest request);
    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(source = "cliente.id", target = "clienteId")
    FavoritoResponse toResponse(Favorito favorito);
}
