package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.parcela.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.entity.Parcela;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusParcela;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.ParcelaMapper;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import com.reginaldo.apisistemavendacarros.repository.ParcelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParcelaService {

    private static final int DIAS_ENTRE_VENCIMENTOS = 30;

    private final ParcelaRepository parcelaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ParcelaMapper mapper;

    @Transactional
    public List<ParcelaResponse> criarParcelas (UUID pagamentoId, int quantidade) {
        // Bloqueia o pagamento para duas requisições simultâneas não gerarem parcelas duplicadas
        Pagamento pagamento = pagamentoRepository.findByIdComBloqueio(pagamentoId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        if (pagamento.getStatus() != StatusPagamento.APROVADO) {
            throw new ValorInvalidoException("Somente pagamentos aprovados podem ser parcelados. Status atual: " + pagamento.getStatus());
        }

        // A Stripe já cobrou o valor inteiro: parcelas aqui seriam uma dívida que não existe
        if (pagamento.getIdExterno() != null) {
            throw new ValorInvalidoException("Pagamento com cartão já foi cobrado integralmente e não pode ser parcelado");
        }

        if (quantidade < 1 || quantidade > 12) {
            throw new ValorInvalidoException("Quantidade de parcelas deve estar entre 1 e 12");
        }

        if (parcelaRepository.existsByPagamentoId(pagamentoId)) {
            throw new ConflitoException("Pagamento já possui parcelas");
        }

        if (pagamento.getDataPagamento() == null) {
            throw new ValorInvalidoException("Pagamento aprovado sem data de aprovação");
        }

        List<BigDecimal> valores = dividirValor(pagamento.getValor(), quantidade);
        LocalDate dataAprovacao = pagamento.getDataPagamento().toLocalDate();

        List<Parcela> parcelas = new ArrayList<>();
        for (int numero = 1; numero <= quantidade; numero++) {
            Parcela parcela = new Parcela();
            parcela.setPagamento(pagamento);
            parcela.setNumero(numero);
            parcela.setValor(valores.get(numero - 1));
            parcela.setStatus(StatusParcela.PENDENTE);
            parcela.setDataVencimento(dataAprovacao.plusDays((long) DIAS_ENTRE_VENCIMENTOS * numero));
            parcelas.add(parcela);
        }

        return parcelaRepository.saveAll(parcelas).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParcelaResponse> listarPorPagamento (Usuario usuario, UUID pagamentoId) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pagamento não encontrado"));

        verificarAcesso(usuario, pagamento);

        return parcelaRepository.findByPagamentoIdOrderByNumeroAsc(pagamentoId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParcelaResponse buscarPorId (Usuario usuario, UUID id) {
        Parcela parcela = parcelaRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Parcela não encontrada"));

        verificarAcesso(usuario, parcela.getPagamento());

        return mapper.toResponse(parcela);
    }

    @Transactional
    public ParcelaResponse pagar (UUID id) {
        Parcela parcela = buscarParcelaPendente(id);

        parcela.setStatus(StatusParcela.PAGA);
        parcela.setDataPagamento(LocalDate.now());

        return mapper.toResponse(parcela);
    }

    @Transactional
    public ParcelaResponse cancelar (UUID id) {
        Parcela parcela = buscarParcelaPendente(id);

        parcela.setStatus(StatusParcela.CANCELADA);

        return mapper.toResponse(parcela);
    }

    // A sobra de centavos vai para a última parcela, para a soma bater com o valor do pagamento
    private List<BigDecimal> dividirValor (BigDecimal total, int quantidade) {
        BigDecimal valorBase = total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
        BigDecimal ultima = total.subtract(valorBase.multiply(BigDecimal.valueOf(quantidade - 1)));

        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 1; i < quantidade; i++) {
            valores.add(valorBase);
        }
        valores.add(ultima);
        return valores;
    }

    private void verificarAcesso (Usuario usuario, Pagamento pagamento) {
        if (usuario.getPerfil() == PerfilUsuario.ADMINISTRADOR) {
            return;
        }

        UUID donoId = pagamento.getCompra().getCliente().getUsuario().getId();
        if (!donoId.equals(usuario.getId())) {
            // Convertida em 403 pelo Spring Security
            throw new AccessDeniedException("Parcela não pertence ao cliente autenticado");
        }
    }

    private Parcela buscarParcelaPendente (UUID id) {
        Parcela parcela = parcelaRepository.findByIdComBloqueio(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Parcela não encontrada"));

        if (parcela.getStatus() != StatusParcela.PENDENTE) {
            throw new ValorInvalidoException("Somente parcelas pendentes podem ser alteradas. Status atual: " + parcela.getStatus());
        }

        return parcela;
    }
}
