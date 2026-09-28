package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.UsuarioMapper;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UsuarioResponse cadastro (UsuarioRequest request) {
        String senhaHash = passwordEncoder.encode(request.senha());

        Usuario usuario = mapper.toEntity(request);
        usuario.setPerfil(PerfilUsuario.USUARIO);
        usuario.setSenha(senhaHash);
        usuario.setProvedor(ProvedorAutenticacao.LOCAL);

        usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    public List<UsuarioResponse> listar () {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public UsuarioResponse buscarPorId (UUID id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));

        return mapper.toResponse(usuario);
    }

    public UsuarioResponse atualizar (UUID id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));

        mapper.atualizar(request, usuario);

        usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    public void excluir (UUID id) {
        usuarioRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));

        usuarioRepository.deleteById(id);
    }
}
