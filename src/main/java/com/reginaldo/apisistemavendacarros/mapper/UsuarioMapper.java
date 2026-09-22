package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    Usuario toEntity(UsuarioRequest request);
    UsuarioResponse toResponse(Usuario usuario);
}
