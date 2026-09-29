package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.UsuarioMapper;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse cadastro (UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException("E-mail já cadastrado");
        }

        Usuario usuario = mapper.toEntity(request);
        usuario.setPerfil(PerfilUsuario.USUARIO);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
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
        return mapper.toResponse(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse atualizar (UUID id, UsuarioRequest request) {
        Usuario usuario = buscarUsuario(id);

        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflitoException("E-mail já cadastrado");
        }

        // O login pelo Google localiza o usuário pelo e-mail; trocar o e-mail criaria uma segunda conta
        if (usuario.getProvedor() != ProvedorAutenticacao.LOCAL && !usuario.getEmail().equals(request.email())) {
            throw new ValorInvalidoException("E-mail de conta vinculada ao " + usuario.getProvedor() + " não pode ser alterado");
        }

        mapper.atualizar(request, usuario);
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    // Remove junto o Cliente do usuário, pelas regras do ClienteService (bloqueia se houver compras)
    @Transactional
    public void excluir (UUID id) {
        Usuario usuario = buscarUsuario(id);

        clienteService.excluirSeExistir(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarUsuario (UUID id) {
        return usuarioRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
