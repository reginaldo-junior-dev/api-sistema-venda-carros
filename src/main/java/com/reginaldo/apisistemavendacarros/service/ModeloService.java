package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.ModeloRequest;
import com.reginaldo.apisistemavendacarros.dto.ModeloResponse;
import com.reginaldo.apisistemavendacarros.entity.Marca;
import com.reginaldo.apisistemavendacarros.entity.Modelo;
import com.reginaldo.apisistemavendacarros.mapper.ModeloMapper;
import com.reginaldo.apisistemavendacarros.repository.MarcaRepository;
import com.reginaldo.apisistemavendacarros.repository.ModeloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModeloService {

    private final ModeloRepository modeloRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloMapper mapper;

    public ModeloResponse cadastro (ModeloRequest request) {
        Marca marca = marcaRepository.findById(request.marcaId()).orElseThrow(() ->
                new RuntimeException("Marca não encontrada"));

        Modelo modelo = mapper.toEntity(request);
        modelo.setMarca(marca);

        modeloRepository.save(modelo);
        return mapper.toResponse(modelo);
    }

    public List<ModeloResponse> listar () {
        List<Modelo> modelos = modeloRepository.findAll();
        return modelos.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ModeloResponse buscarPorId (UUID id) {
        Modelo modelo = modeloRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Modelo não encontrado"));

        return mapper.toResponse(modelo);
    }

    public ModeloResponse atualizar (UUID id, ModeloRequest request) {
        Modelo modelo = modeloRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Modelo não encontrado"));

        Marca marca = marcaRepository.findById(request.marcaId()).orElseThrow(() ->
                new RuntimeException("Marca não encontrada"));

        mapper.atualizar(request, modelo);
        modelo.setMarca(marca);

        modeloRepository.save(modelo);

        return mapper.toResponse(modelo);
    }

    public void excluir (UUID id) {
        modeloRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Modelo não encontrado"));

        modeloRepository.deleteById(id);
    }
}
