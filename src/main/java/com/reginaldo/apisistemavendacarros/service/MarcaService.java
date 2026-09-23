package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.MarcaRequest;
import com.reginaldo.apisistemavendacarros.dto.MarcaResponse;
import com.reginaldo.apisistemavendacarros.entity.Marca;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.MarcaMapper;
import com.reginaldo.apisistemavendacarros.repository.MarcaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository marcaRepository;
    private final MarcaMapper mapper;

    public MarcaResponse cadastro (MarcaRequest request) {
        Marca marca = mapper.toEntity(request);
        Marca marcaSalva = marcaRepository.save(marca);
        return mapper.toResponse(marcaSalva);
    }

    public List<MarcaResponse> lista () {
        List<Marca> listar = marcaRepository.findAll();

        return listar.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public MarcaResponse buscarPorId (UUID id) {
        Marca marca = marcaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Marca não encontrada"));

        return mapper.toResponse(marca);
    }

    public MarcaResponse atualizar(UUID id, MarcaRequest request) {

        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Marca não encontrada"));

        mapper.atualizar(request, marca);

        marcaRepository.save(marca);

        return mapper.toResponse(marca);
    }

    public void excluir (UUID id) {
        marcaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Marca não encontrada"));

        marcaRepository.deleteById(id);

    }
}
