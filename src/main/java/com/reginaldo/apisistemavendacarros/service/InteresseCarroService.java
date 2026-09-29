package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
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

        // nome, email e telefone ficam gravados como informados agora, sem vínculo com os dados atuais do Cliente
        InteresseCarro interesseCarro = mapper.toEntity(request);
        interesseCarro.setStatus(StatusInteresse.NOVO);
        interesseCarro.setCarro(carro);
        interesseCarro.setCliente(cliente);

        interesseCarroRepository.save(interesseCarro);

        return mapper.toResponse(interesseCarro);
    }

    public List<InteresseCarroResponse> listarPorUsuario (UUID usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return interesseCarroRepository.findByClienteId(cliente.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<InteresseCarroResponse> listar () {
        List<InteresseCarro> interesses = interesseCarroRepository.findAll();
        return interesses.stream()
                .map(mapper::toResponse)
                .toList();
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
