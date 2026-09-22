package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PagamentoMapper {
    Pagamento toEntity(PagamentoRequest request);

    void atualizar(PagamentoRequest request, @MappingTarget Pagamento pagamento);

    @Mapping(source = "compra.id", target = "compraId")
    PagamentoResponse toResponse(Pagamento pagamento);
}
