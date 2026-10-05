package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Sem expiração, uma compra abandonada deixaria o carro reservado para sempre
@Slf4j
@Service
public class ExpiracaoCompraService {

    private final CompraRepository compraRepository;
    private final CompraService compraService;
    private final Duration tempoExpiracao;

    public ExpiracaoCompraService(CompraRepository compraRepository,
                                  CompraService compraService,
                                  @Value("${compra.expiracao.tempo:30m}") Duration tempoExpiracao) {
        this.compraRepository = compraRepository;
        this.compraService = compraService;
        this.tempoExpiracao = tempoExpiracao;
    }

    // Cada compra é cancelada na própria transação: uma falha não desfaz as outras
    @Scheduled(fixedDelayString = "${compra.expiracao.intervalo:PT1M}")
    public void expirarCompras() {
        LocalDateTime limite = LocalDateTime.now().minus(tempoExpiracao);
        List<UUID> expiradas = compraRepository.findIdsPendentesCriadasAntesDe(
                limite, StatusCompra.PENDENTE, StatusPagamento.PENDENTE);

        for (UUID compraId : expiradas) {
            try {
                compraService.expirar(compraId);
                log.info("Compra {} expirada após {} sem pagamento: cancelada e carro liberado", compraId, tempoExpiracao);
            } catch (RuntimeException e) {
                // Ex.: Stripe processando ou fora do ar. A compra é reavaliada na próxima execução
                log.warn("Compra {} não pôde ser expirada agora: {}", compraId, e.getMessage());
            }
        }
    }
}
