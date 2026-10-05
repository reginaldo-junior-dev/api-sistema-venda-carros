package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.endereco.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.endereco.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    Endereco toEntity(EnderecoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    void atualizar(EnderecoRequest request, @MappingTarget Endereco endereco);

    @Mapping(source = "cliente.id", target = "clienteId")
    EnderecoResponse toResponse(Endereco endereco);
}
