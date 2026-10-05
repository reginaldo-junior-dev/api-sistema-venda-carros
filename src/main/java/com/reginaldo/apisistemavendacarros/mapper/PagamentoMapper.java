package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagamentoMapper {
    @Mapping(source = "compra.id", target = "compraId")
    @Mapping(target = "clientSecret", ignore = true)
    PagamentoResponse toResponse(Pagamento pagamento);

    @Mapping(source = "pagamento.compra.id", target = "compraId")
    PagamentoResponse toResponse(Pagamento pagamento, String clientSecret);
}
