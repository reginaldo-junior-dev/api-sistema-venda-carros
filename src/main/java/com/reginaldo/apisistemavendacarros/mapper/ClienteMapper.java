package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    Cliente toEntity(ClienteRequest request);

    void atualizar(ClienteRequest request, @MappingTarget Cliente cliente);

    ClienteResponse toResponse(Cliente cliente);
}
