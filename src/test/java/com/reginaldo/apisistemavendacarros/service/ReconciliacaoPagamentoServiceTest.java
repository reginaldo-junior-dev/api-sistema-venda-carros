package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// A aprovação em si (compra aprovada, carro vendido) já é coberta pelos testes do webhook,
// que usam o mesmo atualizarPeloPaymentIntent; aqui só a seleção e o isolamento de falhas
class ReconciliacaoPagamentoServiceTest {

    private final PagamentoRepository pagamentoRepository = mock(PagamentoRepository.class);
    private final StripeService stripeService = mock(StripeService.class);
    private final PagamentoService pagamentoService = mock(PagamentoService.class);
    private final ReconciliacaoPagamentoService reconciliacao =
            new ReconciliacaoPagamentoService(pagamentoRepository, stripeService, pagamentoService);

    @Test
    void aplicaOStatusDaStripeAosCartoesPendentes() {
        PaymentIntent pago = new PaymentIntent();
        when(pagamentoRepository.findIdsExternosPorStatus(StatusPagamento.PENDENTE)).thenReturn(List.of("pi_pago"));
        when(stripeService.buscarPagamento("pi_pago")).thenReturn(Optional.of(pago));

        reconciliacao.reconciliarPendentes();

        verify(pagamentoService).atualizarPeloPaymentIntent(pago);
    }

    @Test
    void falhaEmUmPagamentoNaoImpedeOsOutros() {
        PaymentIntent segundo = new PaymentIntent();
        when(pagamentoRepository.findIdsExternosPorStatus(StatusPagamento.PENDENTE)).thenReturn(List.of("pi_falha", "pi_ok"));
        when(stripeService.buscarPagamento("pi_falha")).thenThrow(new RuntimeException("Stripe fora do ar"));
        when(stripeService.buscarPagamento("pi_ok")).thenReturn(Optional.of(segundo));

        reconciliacao.reconciliarPendentes();

        verify(pagamentoService).atualizarPeloPaymentIntent(segundo);
    }

    @Test
    void pagamentoQueNaoExisteNaStripeEIgnorado() {
        when(pagamentoRepository.findIdsExternosPorStatus(StatusPagamento.PENDENTE)).thenReturn(List.of("pi_inexistente"));
        when(stripeService.buscarPagamento("pi_inexistente")).thenReturn(Optional.empty());

        reconciliacao.reconciliarPendentes();

        verify(pagamentoService, never()).atualizarPeloPaymentIntent(any());
    }
}
