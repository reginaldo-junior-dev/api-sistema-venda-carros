package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.mapper.CompraMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;
    private final CarroRepository carroRepository;
    private final CompraMapper mapper;

    public CompraResponse cadastro (CompraRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RuntimeException("Cliente não encontrado"));

        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RuntimeException("Carro não encontrado"));

        Compra compra = mapper.toEntity(request);
        compra.setValorTotal(carro.getPreco());
        compra.setStatus(StatusCompra.PENDENTE);
        compra.setCliente(cliente);
        compra.setCarro(carro);

        compraRepository.save(compra);

        return mapper.toResponse(compra);
    }

    public List<CompraResponse> listar () {
        List<Compra> compras = compraRepository.findAll();
        return compras.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CompraResponse buscarPorId (UUID id) {
        Compra compra = compraRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Compra não encontrada"));

        return mapper.toResponse(compra);
    }

    public CompraResponse atualizar (UUID id, CompraRequest request) {
        Compra compra = compraRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Compra não encontrada"));

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElseThrow(() ->
                new RuntimeException("Cliente não encontrado"));

        Carro carro = carroRepository.findById(request.carroId()).orElseThrow(() ->
                new RuntimeException("Carro não encontrado"));

        mapper.atualizar(request, compra);
        compra.setValorTotal(carro.getPreco());
        compra.setCliente(cliente);
        compra.setCarro(carro);

        compraRepository.save(compra);

        return mapper.toResponse(compra);
    }

    public void excluir (UUID id) {
        compraRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Compra não encontrada"));

        compraRepository.deleteById(id);
    }
}
