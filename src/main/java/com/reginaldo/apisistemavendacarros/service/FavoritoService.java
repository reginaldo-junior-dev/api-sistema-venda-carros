package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.favorito.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Favorito;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.FavoritoMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.FavoritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final CarroRepository carroRepository;
    private final ClienteRepository clienteRepository;
    private final FavoritoMapper mapper;

    @Transactional
    public FavoritoResponse cadastro (UUID usuarioId, UUID carroId) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        Carro carro = carroRepository.findById(carroId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        if (carro.getStatus() == StatusCarro.VENDIDO) {
            throw new ValorInvalidoException("Não é possível favoritar um carro vendido");
        }

        if (favoritoRepository.existsByClienteIdAndCarroId(cliente.getId(), carroId)) {
            throw new ConflitoException("Carro já está nos favoritos");
        }

        Favorito favorito = new Favorito();
        favorito.setCliente(cliente);
        favorito.setCarro(carro);

        favoritoRepository.save(favorito);

        return mapper.toResponse(favorito);
    }

    public Page<FavoritoResponse> listarPorUsuario (UUID usuarioId, Pageable pageable) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        return favoritoRepository.findByClienteId(cliente.getId(), pageable).map(mapper::toResponse);
    }

    @Transactional
    public void excluir (UUID usuarioId, UUID carroId) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        Favorito favorito = favoritoRepository.findByClienteIdAndCarroId(cliente.getId(), carroId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Favorito não encontrado"));

        favoritoRepository.delete(favorito);
    }

    private Cliente buscarClienteDoUsuario (UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
