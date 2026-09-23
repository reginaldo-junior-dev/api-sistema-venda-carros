package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.InteresseCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.InteresseCarroMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.InteresseCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteresseCarroService {

    private final InteresseCarroRepository interesseCarroRepository;
    private final CarroRepository carroRepository;
    private final ClienteRepository clienteRepository;
    private final InteresseCarroMapper mapper;

    public InteresseCarroResponse cadastro (InteresseCarroRequest request) {
        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        InteresseCarro interesseCarro = mapper.toEntity(request);
        interesseCarro.setStatus(StatusInteresse.NOVO);
        interesseCarro.setCarro(carro);
        interesseCarro.setCliente(cliente);

        interesseCarroRepository.save(interesseCarro);

        return mapper.toResponse(interesseCarro);
    }

    public List<InteresseCarroResponse> listar () {
        List<InteresseCarro> interesses = interesseCarroRepository.findAll();
        return interesses.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public InteresseCarroResponse buscarPorId (UUID id) {
        InteresseCarro interesseCarro = interesseCarroRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Interesse não encontrado"));

        return mapper.toResponse(interesseCarro);
    }

    public InteresseCarroResponse atualizar (UUID id, InteresseCarroRequest request) {
        InteresseCarro interesseCarro = interesseCarroRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Interesse não encontrado"));

        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        mapper.atualizar(request, interesseCarro);
        interesseCarro.setCarro(carro);
        interesseCarro.setCliente(cliente);

        interesseCarroRepository.save(interesseCarro);

        return mapper.toResponse(interesseCarro);
    }

    public void excluir (UUID id) {
        interesseCarroRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Interesse não encontrado"));

        interesseCarroRepository.deleteById(id);
    }
}
