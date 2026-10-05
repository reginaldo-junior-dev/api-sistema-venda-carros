package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.compra.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.event.CompraAprovadaEvent;
import com.reginaldo.apisistemavendacarros.event.CompraExpiradaEvent;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.CompraMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final StripeService stripeService;
    private final ApplicationEventPublisher eventPublisher;

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

        // Preço copiado do carro: mudanças futuras no preço não afetam a compra
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setCarro(carro);
        compra.setValorTotal(carro.getPreco());
        compra.setStatus(StatusCompra.PENDENTE);

        compraRepository.save(compra);

        carro.setStatus(StatusCarro.RESERVADO);

        return mapper.toResponse(compra);
    }

    public Page<CompraResponse> listarPorUsuario (UUID usuarioId, Pageable pageable) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return compraRepository.findByClienteId(cliente.getId(), pageable).map(mapper::toResponse);
    }

    public Page<CompraResponse> listar (Pageable pageable) {
        return compraRepository.findAll(pageable).map(mapper::toResponse);
    }

    public CompraResponse buscarPorId (UUID id) {
        Compra compra = compraRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Compra não encontrada"));

        return mapper.toResponse(compra);
    }

    // Sem endpoint: a compra só é aprovada pela aprovação de um pagamento
    @Transactional
    public CompraResponse aprovar (UUID id) {
        Compra compra = buscarCompraPendente(id);
        Carro carro = compra.getCarro();

        if (carro.getStatus() != StatusCarro.RESERVADO) {
            throw new ValorInvalidoException("Carro da compra não está reservado");
        }

        compra.setStatus(StatusCompra.APROVADA);
        carro.setStatus(StatusCarro.VENDIDO);

        // E-mail sai só após o commit (EmailListener)
        eventPublisher.publishEvent(new CompraAprovadaEvent(compra.getCliente().getUsuario().getEmail(),
                compra.getCliente().getUsuario().getNomeCompleto(), carro.getNome(), compra.getValorTotal()));

        return mapper.toResponse(compra);
    }

    @Transactional
    public CompraResponse cancelar (UUID id) {
        return mapper.toResponse(cancelarPendente(id));
    }

    // Chamado pelo job de expiração: cancela e avisa o cliente (e-mail após o commit)
    @Transactional
    public void expirar (UUID id) {
        Compra compra = cancelarPendente(id);

        eventPublisher.publishEvent(new CompraExpiradaEvent(compra.getCliente().getUsuario().getEmail(),
                compra.getCliente().getUsuario().getNomeCompleto(), compra.getCarro().getNome(), compra.getValorTotal()));
    }

    private Compra cancelarPendente (UUID id) {
        Compra compra = buscarCompraPendente(id);
        Carro carro = compra.getCarro();

        // Cancela na Stripe antes de alterar a compra: se ela já estiver processando, nada muda
        pagamentoRepository.findByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE)
                .forEach(pagamento -> {
                    if (pagamento.getIdExterno() != null) {
                        stripeService.garantirCancelamento(pagamento.getIdExterno());
                    }
                    pagamento.setStatus(StatusPagamento.CANCELADO);
                });

        compra.setStatus(StatusCompra.CANCELADA);

        if (carro.getStatus() == StatusCarro.RESERVADO) {
            carro.setStatus(StatusCarro.DISPONIVEL);
        }

        return compra;
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
