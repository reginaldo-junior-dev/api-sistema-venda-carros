package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.CompraMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final List<StatusCompra> STATUS_ATIVOS = List.of(StatusCompra.PENDENTE, StatusCompra.APROVADA);

    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;
    private final CarroRepository carroRepository;
    private final PagamentoRepository pagamentoRepository;
    private final CompraMapper mapper;

    @Transactional
    public CompraResponse cadastro (UUID usuarioId, UUID carroId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        Carro carro = carroRepository.findByIdComBloqueio(carroId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Carro não encontrado"));

        if (carro.getStatus() != StatusCarro.DISPONIVEL) {
            throw new ValorInvalidoException("Carro não está disponível para compra");
        }

        if (compraRepository.existsByCarroIdAndStatusIn(carroId, STATUS_ATIVOS)) {
            throw new ConflitoException("Carro já possui uma compra em andamento");
        }

        // O valor é copiado do preço atual; alterações futuras no preço do carro não afetam a compra
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setCarro(carro);
        compra.setValorTotal(carro.getPreco());
        compra.setStatus(StatusCompra.PENDENTE);

        compraRepository.save(compra);

        carro.setStatus(StatusCarro.RESERVADO);

        return mapper.toResponse(compra);
    }

    public List<CompraResponse> listarPorUsuario (UUID usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return compraRepository.findByClienteId(cliente.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<CompraResponse> listar () {
        List<Compra> compras = compraRepository.findAll();
        return compras.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public CompraResponse buscarPorId (UUID id) {
        Compra compra = compraRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Compra não encontrada"));

        return mapper.toResponse(compra);
    }

    // Sem endpoint próprio: a compra só é aprovada pela aprovação de um Pagamento (PagamentoService.aprovar)
    @Transactional
    public CompraResponse aprovar (UUID id) {
        Compra compra = buscarCompraPendente(id);
        Carro carro = compra.getCarro();

        if (carro.getStatus() != StatusCarro.RESERVADO) {
            throw new ValorInvalidoException("Carro da compra não está reservado");
        }

        compra.setStatus(StatusCompra.APROVADA);
        carro.setStatus(StatusCarro.VENDIDO);

        return mapper.toResponse(compra);
    }

    @Transactional
    public CompraResponse cancelar (UUID id) {
        Compra compra = buscarCompraPendente(id);
        Carro carro = compra.getCarro();

        compra.setStatus(StatusCompra.CANCELADA);

        if (carro.getStatus() == StatusCarro.RESERVADO) {
            carro.setStatus(StatusCarro.DISPONIVEL);
        }

        // Pagamentos pendentes não podem mais ser aprovados; RECUSADO/CANCELADO ficam como estão
        pagamentoRepository.findByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE)
                .forEach(pagamento -> pagamento.setStatus(StatusPagamento.CANCELADO));

        return mapper.toResponse(compra);
    }

    private Compra buscarCompraPendente (UUID id) {
        Compra compra = compraRepository.findByIdComBloqueio(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Compra não encontrada"));

        if (compra.getStatus() != StatusCompra.PENDENTE) {
            throw new ValorInvalidoException("Somente compras pendentes podem ser alteradas. Status atual: " + compra.getStatus());
        }

        return compra;
    }
}
