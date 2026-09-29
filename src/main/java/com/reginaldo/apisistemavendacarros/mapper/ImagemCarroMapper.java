package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ImagemCarroMapper {

    @Mapping(source = "carro.id", target = "carroId")
    @Mapping(target = "url", ignore = true)
    ImagemCarroResponse toResponse(ImagemCarro imagemCarro);
}
