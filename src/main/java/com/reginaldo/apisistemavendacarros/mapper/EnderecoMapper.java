package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {
    Endereco toEntity(EnderecoRequest request);

    void atualizar(EnderecoRequest request, @MappingTarget Endereco endereco);

    EnderecoResponse toResponse(Endereco endereco);
}
