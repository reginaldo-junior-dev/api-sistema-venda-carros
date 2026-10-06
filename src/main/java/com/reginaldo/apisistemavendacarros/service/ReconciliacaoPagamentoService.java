package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Garantia para quando o webhook da Stripe não chega (endpoint fora do ar, segredo errado):
// sem ela, um cartão aprovado na Stripe deixaria a compra pendente e o carro reservado para sempre,
// já que a expiração não cancela compras com pagamento pendente
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliacaoPagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final StripeService stripeService;
    private final PagamentoService pagamentoService;

    // Mesmo caminho do webhook: status consultado na Stripe e aplicado com bloqueio, então rodar junto com ele é seguro.
    // Cada pagamento na própria transação: uma falha não impede os outros
    @Scheduled(fixedDelayString = "${pagamento.reconciliacao.intervalo:PT1M}")
    public void reconciliarPendentes() {
        for (String idExterno : pagamentoRepository.findIdsExternosPorStatus(StatusPagamento.PENDENTE)) {
            try {
                stripeService.buscarPagamento(idExterno).ifPresent(pagamentoService::atualizarPeloPaymentIntent);
            } catch (RuntimeException e) {
                // Ex.: Stripe fora do ar. O pagamento é conferido de novo na próxima execução
                log.warn("Pagamento {} não pôde ser conferido na Stripe agora: {}", idExterno, e.getMessage());
            }
        }
    }
}
