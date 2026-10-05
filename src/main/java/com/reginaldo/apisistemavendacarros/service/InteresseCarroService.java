package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.interesse.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.interesse.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.InteresseCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.InteresseCarroMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.InteresseCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteresseCarroService {

    private final InteresseCarroRepository interesseCarroRepository;
    private final CarroRepository carroRepository;
    private final ClienteRepository clienteRepository;
    private final InteresseCarroMapper mapper;

    @Transactional
    public InteresseCarroResponse cadastro (UUID usuarioId, UUID carroId, InteresseCarroRequest request) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        Carro carro = carroRepository.findById(carroId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        if (carro.getStatus() == StatusCarro.VENDIDO) {
            throw new ValorInvalidoException("Não é possível registrar interesse em um carro vendido");
        }

        // Contato gravado como informado agora, sem vínculo com os dados atuais do cliente
        InteresseCarro interesseCarro = mapper.toEntity(request);
        interesseCarro.setStatus(StatusInteresse.NOVO);
        interesseCarro.setCarro(carro);
        interesseCarro.setCliente(cliente);

        interesseCarroRepository.save(interesseCarro);

        return mapper.toResponse(interesseCarro);
    }

    public Page<InteresseCarroResponse> listarPorUsuario (UUID usuarioId, Pageable pageable) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return interesseCarroRepository.findByClienteId(cliente.getId(), pageable).map(mapper::toResponse);
    }

    public Page<InteresseCarroResponse> listar (Pageable pageable) {
        return interesseCarroRepository.findAll(pageable).map(mapper::toResponse);
    }

    public InteresseCarroResponse buscarPorId (UUID id) {
        return mapper.toResponse(buscarInteresse(id));
    }

    @Transactional
    public InteresseCarroResponse atualizarStatus (UUID id, StatusInteresse status) {
        InteresseCarro interesseCarro = buscarInteresse(id);

        interesseCarro.setStatus(status);

        interesseCarroRepository.save(interesseCarro);

        return mapper.toResponse(interesseCarro);
    }

    private InteresseCarro buscarInteresse (UUID id) {
        return interesseCarroRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Interesse não encontrado"));
    }
}
