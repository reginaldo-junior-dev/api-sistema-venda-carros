package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagamentoMapper {
    Pagamento toEntity(PagamentoRequest request);

    @Mapping(source = "compra.id", target = "compraId")
    PagamentoResponse toResponse(Pagamento pagamento);
}
