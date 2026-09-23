package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.EnderecoMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final ClienteRepository clienteRepository;
    private final EnderecoMapper mapper;

    public EnderecoResponse cadastro (EnderecoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        Endereco endereco = mapper.toEntity(request);
        endereco.setCliente(cliente);

        enderecoRepository.save(endereco);

        return mapper.toResponse(endereco);
    }

    public List<EnderecoResponse> listar () {
        List<Endereco> enderecos = enderecoRepository.findAll();
        return enderecos.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public EnderecoResponse buscarPorId (UUID id) {
        Endereco endereco = enderecoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Endereço não encontrado"));

        return mapper.toResponse(endereco);
    }

    public EnderecoResponse atualizar (UUID id, EnderecoRequest request) {
        Endereco endereco = enderecoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Endereço não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        mapper.atualizar(request, endereco);
        endereco.setCliente(cliente);

        enderecoRepository.save(endereco);

        return mapper.toResponse(endereco);
    }

    public void excluir (UUID id) {
        enderecoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Endereço não encontrado"));

        enderecoRepository.deleteById(id);
    }
}
