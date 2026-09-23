package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.FavoritoRequest;
import com.reginaldo.apisistemavendacarros.dto.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Favorito;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.FavoritoMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final CarroRepository carroRepository;
    private final ClienteRepository clienteRepository;
    private final FavoritoMapper mapper;

    public FavoritoResponse cadastro (FavoritoRequest request) {
        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        Favorito favorito = mapper.toEntity(request);
        favorito.setCarro(carro);
        favorito.setCliente(cliente);

        favoritoRepository.save(favorito);

        return mapper.toResponse(favorito);
    }

    public List<FavoritoResponse> listar () {
        List<Favorito> favoritos = favoritoRepository.findAll();
        return favoritos.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public FavoritoResponse buscarPorId (UUID id) {
        Favorito favorito = favoritoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Favorito não encontrado"));

        return mapper.toResponse(favorito);
    }

    public FavoritoResponse atualizar (UUID id, FavoritoRequest request) {
        Favorito favorito = favoritoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Favorito não encontrado"));

        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        mapper.atualizar(request, favorito);
        favorito.setCarro(carro);
        favorito.setCliente(cliente);

        favoritoRepository.save(favorito);

        return mapper.toResponse(favorito);
    }

    public void excluir (UUID id) {
        favoritoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Favorito não encontrado"));

        favoritoRepository.deleteById(id);
    }
}
