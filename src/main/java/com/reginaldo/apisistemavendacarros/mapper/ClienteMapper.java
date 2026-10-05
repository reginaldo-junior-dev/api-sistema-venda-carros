package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    void atualizar(ClienteRequest request, @MappingTarget Cliente cliente);

    @Mapping(source = "usuario.id", target = "usuarioId")
    ClienteResponse toResponse(Cliente cliente);
}
