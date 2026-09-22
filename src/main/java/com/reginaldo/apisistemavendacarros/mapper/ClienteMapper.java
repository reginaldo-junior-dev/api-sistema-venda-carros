package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper {
    Cliente toEntity(ClienteRequest request);
    ClienteResponse toResponse(Cliente cliente);
}
