package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.mapper.PagamentoMapper;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final CompraRepository compraRepository;
    private final PagamentoMapper mapper;

    public PagamentoResponse cadastro (PagamentoRequest request) {
        Compra compra = compraRepository.findById(request.compraId()).orElseThrow(() ->
                new RuntimeException("Compra não encontrada"));

        Pagamento pagamento = mapper.toEntity(request);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setCompra(compra);

        pagamentoRepository.save(pagamento);

        return mapper.toResponse(pagamento);
    }

    public List<PagamentoResponse> listar () {
        List<Pagamento> pagamentos = pagamentoRepository.findAll();
        return pagamentos.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public PagamentoResponse buscarPorId (UUID id) {
        Pagamento pagamento = pagamentoRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Pagamento não encontrado"));

        return mapper.toResponse(pagamento);
    }

    public PagamentoResponse atualizar (UUID id, PagamentoRequest request) {
        Pagamento pagamento = pagamentoRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Pagamento não encontrado"));

        Compra compra = compraRepository.findById(request.compraId()).orElseThrow(() ->
                new RuntimeException("Compra não encontrada"));

        mapper.atualizar(request, pagamento);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setCompra(compra);

        pagamentoRepository.save(pagamento);

        return mapper.toResponse(pagamento);
    }

    public void excluir (UUID id) {
        pagamentoRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Pagamento não encontrado"));

        pagamentoRepository.deleteById(id);
    }
}
