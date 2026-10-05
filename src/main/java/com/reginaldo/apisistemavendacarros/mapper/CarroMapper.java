package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.carro.CarroRequest;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = ImagemCarroMapper.class)
public interface CarroMapper {
    Carro toEntity(CarroRequest request);

    void atualizar(CarroRequest request, @MappingTarget Carro carro);

    @Mapping(source = "modelo.id", target = "modeloId")
    @Mapping(source = "cor.id", target = "corId")
    @Mapping(source = "categoria.id", target = "categoriaId")
    CarroResponse toResponse(Carro carro);
}
