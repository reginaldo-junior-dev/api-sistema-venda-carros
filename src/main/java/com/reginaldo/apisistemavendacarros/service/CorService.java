package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.cor.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.cor.CorResponse;
import com.reginaldo.apisistemavendacarros.entity.Cor;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.CorMapper;
import com.reginaldo.apisistemavendacarros.repository.CorRepository;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorService {

    private final CorRepository corRepository;
    private final CarroRepository carroRepository;
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

        // Mensagem clara em vez do erro genérico de chave estrangeira
        if (carroRepository.existsByCorId(id)) {
            throw new ConflitoException("Cor possui carros cadastrados e não pode ser excluída");
        }

        corRepository.deleteById(id);
    }
}
