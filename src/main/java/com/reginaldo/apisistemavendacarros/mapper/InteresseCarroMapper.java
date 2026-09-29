package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.InteresseCarro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InteresseCarroMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dataInteresse", ignore = true)
    @Mapping(target = "carro", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    InteresseCarro toEntity(InteresseCarroRequest request);

    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(source = "cliente.id", target = "clienteId")
    InteresseCarroResponse toResponse(InteresseCarro interesseCarro);
}
