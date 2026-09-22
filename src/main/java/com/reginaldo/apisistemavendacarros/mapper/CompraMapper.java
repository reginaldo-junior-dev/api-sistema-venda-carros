package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompraMapper {
    Compra toEntity(CompraRequest request);

    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(source = "cliente.id", target = "clienteId")
    CompraResponse toResponse(Compra compra);
}
