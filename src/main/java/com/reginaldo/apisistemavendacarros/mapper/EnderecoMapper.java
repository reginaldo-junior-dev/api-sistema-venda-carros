package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {
    Endereco toEntity(EnderecoRequest request);
    EnderecoResponse toResponse(Endereco endereco);
}
