package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.InteresseCarro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InteresseCarroMapper {
    InteresseCarro toEntity(InteresseCarroRequest request);

    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(source = "cliente.id", target = "clienteId")
    InteresseCarroResponse toResponse(com.reginaldo.apisistemavendacarros.entity.InteresseCarro interesseCarro);
}
