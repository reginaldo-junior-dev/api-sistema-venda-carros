package com.reginaldo.apisistemavendacarros.mapper;

import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    // A senha é criptografada no UsuarioService, nunca copiada direto do request
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
