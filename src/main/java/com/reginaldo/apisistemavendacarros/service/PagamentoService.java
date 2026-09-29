package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.PagamentoMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;
    private final CompraService compraService;
    private final PagamentoMapper mapper;

    @Transactional
    public PagamentoResponse cadastro (UUID usuarioId, PagamentoRequest request) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        // Bloqueia a compra para que duas requisições simultâneas não criem dois pagamentos pendentes
        Compra compra = compraRepository.findByIdComBloqueio(request.compraId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Compra não encontrada"));

        if (!compra.getCliente().getId().equals(cliente.getId())) {
            // Tratada pelo Spring Security e convertida em 403
            throw new AccessDeniedException("Compra não pertence ao cliente autenticado");
        }

        if (compra.getStatus() != StatusCompra.PENDENTE) {
            throw new ValorInvalidoException("Somente compras pendentes podem receber pagamento. Status atual: " + compra.getStatus());
        }

        if (pagamentoRepository.existsByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE)) {
            throw new ConflitoException("Compra já possui um pagamento pendente");
        }

        // idExterno e dataPagamento ficam null: serão preenchidos pela integração e pela aprovação
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(compra);
        pagamento.setMetodo(request.metodo());
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);

        pagamentoRepository.save(pagamento);

        return mapper.toResponse(pagamento);
    }

    public List<PagamentoResponse> listarPorUsuario (UUID usuarioId) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        return pagamentoRepository.findByCompraClienteId(cliente.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<PagamentoResponse> listar () {
        List<Pagamento> pagamentos = pagamentoRepository.findAll();
        return pagamentos.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public PagamentoResponse buscarPorId (UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        return mapper.toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse aprovar (UUID id) {
        UUID compraId = pagamentoRepository.findCompraIdByPagamentoId(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        // Trava a compra antes do pagamento: mesma ordem usada no cancelamento da compra, evitando deadlock
        compraRepository.findByIdComBloqueio(compraId);
        Pagamento pagamento = buscarPagamentoPendente(id);

        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento.setDataPagamento(LocalDateTime.now());

        // Reaproveita a aprovação da Compra (Compra → APROVADA, Carro → VENDIDO).
        // Se ela falhar, a transação inteira é desfeita e o pagamento continua PENDENTE.
        compraService.aprovar(pagamento.getCompra().getId());

        return mapper.toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse recusar (UUID id) {
        Pagamento pagamento = buscarPagamentoPendente(id);

        pagamento.setStatus(StatusPagamento.RECUSADO);

        return mapper.toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse cancelar (UUID id) {
        Pagamento pagamento = buscarPagamentoPendente(id);

        pagamento.setStatus(StatusPagamento.CANCELADO);

        return mapper.toResponse(pagamento);
    }

    private Pagamento buscarPagamentoPendente (UUID id) {
        Pagamento pagamento = pagamentoRepository.findByIdComBloqueio(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            throw new ValorInvalidoException("Somente pagamentos pendentes podem ser alterados. Status atual: " + pagamento.getStatus());
        }

        return pagamento;
    }

    private Cliente buscarClienteDoUsuario (UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
