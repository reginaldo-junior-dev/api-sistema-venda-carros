package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteAtualizacaoRequest;
import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.ClienteMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import com.reginaldo.apisistemavendacarros.repository.FavoritoRepository;
import com.reginaldo.apisistemavendacarros.repository.InteresseCarroRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final FavoritoRepository favoritoRepository;
    private final InteresseCarroRepository interesseCarroRepository;
    private final CompraRepository compraRepository;
    private final ClienteMapper mapper;

    @Transactional
    public ClienteResponse cadastro (UUID usuarioId, ClienteRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));

        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new ConflitoException("Usuário já possui um cliente cadastrado");
        }

        if (clienteRepository.existsByCpf(request.cpf())) {
            throw new ConflitoException("CPF já cadastrado");
        }

        Cliente cliente = mapper.toEntity(request);
        cliente.setUsuario(usuario);

        clienteRepository.save(cliente);

        return mapper.toResponse(cliente);
    }

    public Page<ClienteResponse> listar (Pageable pageable) {
        return clienteRepository.findAll(pageable).map(mapper::toResponse);
    }

    public ClienteResponse buscarPorId (UUID id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return mapper.toResponse(cliente);
    }

    public ClienteResponse buscarPorUsuario (UUID usuarioId) {
        return mapper.toResponse(buscarClienteDoUsuario(usuarioId));
    }

    @Transactional
    public ClienteResponse atualizarPorUsuario (UUID usuarioId, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        mapper.atualizar(request, cliente);

        clienteRepository.save(cliente);

        return mapper.toResponse(cliente);
    }

    @Transactional
    public void excluirPorUsuario (UUID usuarioId) {
        excluirComDependentes(buscarClienteDoUsuario(usuarioId));
    }

    @Transactional
    public void excluir (UUID id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        excluirComDependentes(cliente);
    }

    // Usado na exclusão do usuário
    @Transactional
    public void excluirSeExistir (UUID usuarioId) {
        clienteRepository.findByUsuarioId(usuarioId).ifPresent(this::excluirComDependentes);
    }

    // Compras são histórico financeiro e impedem a exclusão.
    // Favoritos, interesses e endereços são apagados junto
    private void excluirComDependentes (Cliente cliente) {
        if (compraRepository.existsByClienteId(cliente.getId())) {
            throw new ValorInvalidoException("Cliente possui compras registradas e não pode ser excluído");
        }

        favoritoRepository.deleteByClienteId(cliente.getId());
        interesseCarroRepository.deleteByClienteId(cliente.getId());
        enderecoRepository.deleteByClienteId(cliente.getId());
        clienteRepository.delete(cliente);
    }

    private Cliente buscarClienteDoUsuario (UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
