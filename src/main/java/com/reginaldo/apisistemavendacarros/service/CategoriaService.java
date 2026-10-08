package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.categoria.CategoriaRequest;
import com.reginaldo.apisistemavendacarros.dto.categoria.CategoriaResponse;
import com.reginaldo.apisistemavendacarros.entity.Categoria;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.CategoriaMapper;
import com.reginaldo.apisistemavendacarros.repository.CategoriaRepository;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CarroRepository carroRepository;
    private final CategoriaMapper mapper;

    public CategoriaResponse cadastro (CategoriaRequest request) {
        Categoria categoria = mapper.toEntity(request);
        categoriaRepository.save(categoria);

        return mapper.toResponse(categoria);
    }

    public List<CategoriaResponse> listar () {
        List<Categoria> categorias = categoriaRepository.findAll();
        return categorias.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CategoriaResponse buscarPorId (UUID id) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Categoria não encontrada"));

        return mapper.toResponse(categoria);
    }

    public CategoriaResponse atualizar (UUID id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Categoria não encontrada"));

        mapper.atualizar(request, categoria);
        categoriaRepository.save(categoria);

        return mapper.toResponse(categoria);
    }

    public void excluir (UUID id) {
        categoriaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Categoria não encontrada"));

        // Mensagem clara em vez do erro genérico de chave estrangeira
        if (carroRepository.existsByCategoriaId(id)) {
            throw new ConflitoException("Categoria possui carros cadastrados e não pode ser excluída");
        }

        categoriaRepository.deleteById(id);
    }
}
