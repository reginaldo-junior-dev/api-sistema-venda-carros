package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    // A senha nunca é copiada direto do request: o UsuarioService grava sempre a versão criptografada
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "provedor", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "perfil", ignore = true)
    @Mapping(target = "provedor", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    void atualizar(UsuarioRequest request, @MappingTarget Usuario usuario);

    UsuarioResponse toResponse(Usuario usuario);
}
