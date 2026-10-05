package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.event.UsuarioCadastradoEvent;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.UsuarioMapper;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(new UsuarioCadastradoEvent(usuario.getEmail(), usuario.getNomeCompleto()));

        return mapper.toResponse(usuario);
    }

    public Page<UsuarioResponse> listar (Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(mapper::toResponse);
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

        // O login pelo Google localiza o usuário pelo e-mail; trocar o e-mail criaria outra conta
        if (usuario.getProvedor() != ProvedorAutenticacao.LOCAL && !usuario.getEmail().equals(request.email())) {
            throw new ValorInvalidoException("E-mail de conta vinculada ao " + usuario.getProvedor() + " não pode ser alterado");
        }

        mapper.atualizar(request, usuario);
        usuario.setSenha(passwordEncoder.encode(request.senha()));

        usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    // Remove junto o cliente do usuário (bloqueia se houver compras)
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
