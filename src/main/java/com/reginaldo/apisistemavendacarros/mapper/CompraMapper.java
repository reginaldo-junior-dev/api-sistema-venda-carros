package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompraMapper {
    Compra toEntity(CompraRequest request);

    void atualizar(CompraRequest request, @MappingTarget Compra compra);

    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(source = "cliente.id", target = "clienteId")
    CompraResponse toResponse(Compra compra);
}
