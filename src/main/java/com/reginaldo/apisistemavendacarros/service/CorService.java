package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.CorResponse;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.CorMapper;
import com.reginaldo.apisistemavendacarros.repository.CorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorService {

    private final CorRepository corRepository;
    private final CorMapper mapper;

    public CorResponse cadastro (CorRequest request) {
        Cor cor = mapper.toEntity(request);
        corRepository.save(cor);

        return mapper.toResponse(cor);
    }

    public List<CorResponse> listar () {
        List<Cor> cores = corRepository.findAll();
        return cores.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CorResponse buscarPorId (UUID id) {
        Cor cor = corRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cor não encontrada"));

        return mapper.toResponse(cor);
    }

    public CorResponse atualizar (UUID id, CorRequest request) {
        Cor cor = corRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cor não encontrada"));

        mapper.atualizar(request, cor);
        corRepository.save(cor);

        return mapper.toResponse(cor);
    }

    public void excluir (UUID id) {
        corRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cor não encontrada"));

        corRepository.deleteById(id);
    }
}
