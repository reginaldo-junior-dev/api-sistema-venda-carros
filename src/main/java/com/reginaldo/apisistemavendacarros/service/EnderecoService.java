package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.EnderecoMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoMapper mapper;

    @Transactional
    public EnderecoResponse cadastro (UUID usuarioId, EnderecoRequest request) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        Endereco endereco = mapper.toEntity(request);
        endereco.setCliente(cliente);

        if (!enderecoRepository.existsByClienteId(cliente.getId())) {
            endereco.setPrincipal(true);
        } else if (endereco.getPrincipal()) {
            enderecoRepository.desmarcarPrincipal(cliente.getId());
        }

        enderecoRepository.save(endereco);

        return mapper.toResponse(endereco);
    }

    public List<EnderecoResponse> listarPorUsuario (UUID usuarioId) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);
        return listarPorCliente(cliente.getId());
    }

    public List<EnderecoResponse> listarPorClienteId (UUID clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado");
        }

        return listarPorCliente(clienteId);
    }

    public EnderecoResponse buscarPorId (UUID usuarioId, UUID id) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);
        return mapper.toResponse(buscarEnderecoDoCliente(id, cliente.getId()));
    }

    @Transactional
    public EnderecoResponse atualizar (UUID usuarioId, UUID id, EnderecoRequest request) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);
        Endereco endereco = buscarEnderecoDoCliente(id, cliente.getId());

        boolean eraPrincipal = endereco.getPrincipal();

        if (eraPrincipal && !request.principal()) {
            throw new ValorInvalidoException("marque outro endereço como principal");
        }

        if (!eraPrincipal && request.principal()) {
            enderecoRepository.desmarcarPrincipal(cliente.getId());
        }

        mapper.atualizar(request, endereco);

        enderecoRepository.save(endereco);

        return mapper.toResponse(endereco);
    }

    @Transactional
    public void excluir (UUID usuarioId, UUID id) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);
        Endereco endereco = buscarEnderecoDoCliente(id, cliente.getId());

        enderecoRepository.delete(endereco);
        // Garante que o DELETE seja executado antes de promover outro endereço a principal
        enderecoRepository.flush();

        if (endereco.getPrincipal()) {
            enderecoRepository.findFirstByClienteId(cliente.getId())
                    .ifPresent(outro -> outro.setPrincipal(true));
        }
    }

    private List<EnderecoResponse> listarPorCliente (UUID clienteId) {
        return enderecoRepository.findAllByClienteId(clienteId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Cliente buscarClienteDoUsuario (UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    private Endereco buscarEnderecoDoCliente (UUID id, UUID clienteId) {
        return enderecoRepository.findByIdAndClienteId(id, clienteId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Endereço não encontrado"));
    }
}
