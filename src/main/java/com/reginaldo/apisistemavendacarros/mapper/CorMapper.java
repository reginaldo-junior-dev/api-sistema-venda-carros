package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.CorResponse;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CorMapper {
    Cor toEntity(CorRequest request);
    CorResponse toResponse(Cor cor);
}
