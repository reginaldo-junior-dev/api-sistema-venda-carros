package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoCartaoRequest;
import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.enums.MetodoPagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.PagamentoMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final CompraRepository compraRepository;
    private final ClienteRepository clienteRepository;
    private final CompraService compraService;
    private final PagamentoMapper mapper;
    private final StripeService stripeService;

    @Transactional
    public PagamentoResponse cadastro (UUID usuarioId, PagamentoRequest request) {
        Compra compra = buscarCompraParaPagamento(usuarioId, request.compraId());

        if (pagamentoRepository.existsByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE)) {
            throw new ConflitoException("Compra já possui um pagamento pendente");
        }

        Pagamento pagamento = criarPagamentoPendente(compra, request.metodo());

        return mapper.toResponse(pagamento);
    }

    // Se a Stripe falhar, a transação é desfeita e o pagamento PENDENTE não bloqueia novas tentativas
    @Transactional
    public PagamentoResponse pagamentoCartao (UUID usuarioId, PagamentoCartaoRequest request) {
        Compra compra = buscarCompraParaPagamento(usuarioId, request.compraId());

        // Tentativa abandonada (ex.: 3D Secure fechado) não bloqueia uma nova
        cancelarPendentesParaNovaTentativa(compra);

        // O id do pagamento é a chave de idempotência na Stripe
        Pagamento pagamento = criarPagamentoPendente(compra, MetodoPagamento.CARTAO_CREDITO);

        PaymentIntent paymentIntent = stripeService.criarPagamento(compra, pagamento, request);
        pagamento.setIdExterno(paymentIntent.getId());

        // 3D Secure e outros status intermediários ficam PENDENTE até o webhook
        aplicarStatusDoPaymentIntent(pagamento, paymentIntent);

        String clientSecret = stripeService.clientSecretParaAcao(paymentIntent).orElse(null);
        return mapper.toResponse(pagamento, clientSecret);
    }

    // Chamado pelo webhook da Stripe
    @Transactional
    public void atualizarPeloPaymentIntent (PaymentIntent paymentIntent) {
        Optional<UUID> compraId = pagamentoRepository.findCompraIdByIdExterno(paymentIntent.getId());
        if (compraId.isEmpty()) {
            log.warn("Webhook: nenhum pagamento com idExterno {}", paymentIntent.getId());
            return;
        }

        // Trava a compra antes do pagamento, na mesma ordem de aprovar() e do cancelamento, para evitar deadlock
        compraRepository.findByIdComBloqueio(compraId.get());
        Pagamento pagamento = pagamentoRepository.findByIdExternoComBloqueio(paymentIntent.getId()).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        // A Stripe reenvia eventos: pagamento já processado é ignorado
        if (pagamento.getStatus() != StatusPagamento.PENDENTE) {
            if (stripeService.converterStatus(paymentIntent) == StatusPagamento.APROVADO
                    && pagamento.getStatus() != StatusPagamento.APROVADO) {
                log.warn("Webhook: PaymentIntent {} aprovado na Stripe, mas o pagamento {} está {} - verificar estorno",
                        paymentIntent.getId(), pagamento.getId(), pagamento.getStatus());
            }
            return;
        }

        aplicarStatusDoPaymentIntent(pagamento, paymentIntent);
    }

    public Page<PagamentoResponse> listarPorUsuario (UUID usuarioId, Pageable pageable) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        return pagamentoRepository.findByCompraClienteId(cliente.getId(), pageable).map(mapper::toResponse);
    }

    public Page<PagamentoResponse> listar (Pageable pageable) {
        return pagamentoRepository.findAll(pageable).map(mapper::toResponse);
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

        // Trava a compra antes do pagamento, na mesma ordem do cancelamento, para evitar deadlock
        compraRepository.findByIdComBloqueio(compraId);
        Pagamento pagamento = buscarPagamentoPendente(id);

        // Pagamento da Stripe só é aprovado por ela: aprovar à mão venderia o carro sem o dinheiro entrar
        if (pagamento.getIdExterno() != null) {
            throw new ValorInvalidoException("Pagamento com cartão é aprovado pela Stripe e não pode ser aprovado manualmente");
        }

        confirmarAprovacao(pagamento);

        return mapper.toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse recusar (UUID id) {
        Pagamento pagamento = buscarPagamentoPendente(id);

        cancelarNaStripe(pagamento);
        pagamento.setStatus(StatusPagamento.RECUSADO);

        return mapper.toResponse(pagamento);
    }

    @Transactional
    public PagamentoResponse cancelar (UUID id) {
        Pagamento pagamento = buscarPagamentoPendente(id);

        cancelarNaStripe(pagamento);
        pagamento.setStatus(StatusPagamento.CANCELADO);

        return mapper.toResponse(pagamento);
    }

    private Compra buscarCompraParaPagamento (UUID usuarioId, UUID compraId) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        // Bloqueia a compra para duas requisições simultâneas não criarem dois pagamentos pendentes
        Compra compra = compraRepository.findByIdComBloqueio(compraId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Compra não encontrada"));

        if (!compra.getCliente().getId().equals(cliente.getId())) {
            // Convertida em 403 pelo Spring Security
            throw new AccessDeniedException("Compra não pertence ao cliente autenticado");
        }

        if (compra.getStatus() != StatusCompra.PENDENTE) {
            throw new ValorInvalidoException("Somente compras pendentes podem receber pagamento. Status atual: " + compra.getStatus());
        }

        return compra;
    }

    // Pendente da Stripe é cancelado para liberar a nova tentativa; pendente manual continua bloqueando
    private void cancelarPendentesParaNovaTentativa (Compra compra) {
        List<Pagamento> pendentes = pagamentoRepository.findByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE);

        for (Pagamento pendente : pendentes) {
            if (pendente.getIdExterno() == null) {
                throw new ConflitoException("Compra já possui um pagamento pendente");
            }
            cancelarNaStripe(pendente);
            pendente.setStatus(StatusPagamento.CANCELADO);
        }
    }

    // Sem isso o cliente ainda poderia concluir o 3D Secure e ser cobrado
    private void cancelarNaStripe (Pagamento pagamento) {
        if (pagamento.getIdExterno() != null) {
            stripeService.garantirCancelamento(pagamento.getIdExterno());
        }
    }

    private Pagamento criarPagamentoPendente (Compra compra, MetodoPagamento metodo) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(compra);
        pagamento.setMetodo(metodo);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);

        return pagamentoRepository.save(pagamento);
    }

    private void aplicarStatusDoPaymentIntent (Pagamento pagamento, PaymentIntent paymentIntent) {
        switch (stripeService.converterStatus(paymentIntent)) {
            case APROVADO -> confirmarAprovacao(pagamento);
            case RECUSADO -> pagamento.setStatus(StatusPagamento.RECUSADO);
            case CANCELADO -> pagamento.setStatus(StatusPagamento.CANCELADO);
            case PENDENTE -> { }
        }
    }

    private void confirmarAprovacao (Pagamento pagamento) {
        pagamento.setStatus(StatusPagamento.APROVADO);
        pagamento.setDataPagamento(LocalDateTime.now());

        // Se a aprovação da compra falhar, a transação inteira é desfeita e o pagamento continua PENDENTE
        compraService.aprovar(pagamento.getCompra().getId());
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
