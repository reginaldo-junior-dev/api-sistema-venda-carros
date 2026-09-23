package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.ParcelaRequest;
import com.reginaldo.apisistemavendacarros.dto.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.entity.Parcela;
import com.reginaldo.apisistemavendacarros.enums.StatusParcela;
import com.reginaldo.apisistemavendacarros.mapper.ParcelaMapper;
import com.reginaldo.apisistemavendacarros.repository.PagamentoRepository;
import com.reginaldo.apisistemavendacarros.repository.ParcelaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParcelaService {

    private final ParcelaRepository parcelaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ParcelaMapper mapper;

    public ParcelaResponse cadastro (ParcelaRequest request) {
        Pagamento pagamento = pagamentoRepository.findById(request.pagamentoId()).orElseThrow(() ->
                new RuntimeException("Pagamento não encontrado"));

        Parcela parcela = mapper.toEntity(request);
        parcela.setStatus(StatusParcela.PENDENTE);
        parcela.setPagamento(pagamento);

        parcelaRepository.save(parcela);

        return mapper.toResponse(parcela);
    }

    public List<ParcelaResponse> listar () {
        List<Parcela> parcelas = parcelaRepository.findAll();
        return parcelas.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ParcelaResponse buscarPorId (UUID id) {
        Parcela parcela = parcelaRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Parcela não encontrada"));

        return mapper.toResponse(parcela);
    }

    public ParcelaResponse atualizar (UUID id, ParcelaRequest request) {
        Parcela parcela = parcelaRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Parcela não encontrada"));

        Pagamento pagamento = pagamentoRepository.findById(request.pagamentoId()).orElseThrow(() ->
                new RuntimeException("Pagamento não encontrado"));

        mapper.atualizar(request, parcela);
        parcela.setPagamento(pagamento);

        parcelaRepository.save(parcela);

        return mapper.toResponse(parcela);
    }

    public void excluir (UUID id) {
        parcelaRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Parcela não encontrada"));

        parcelaRepository.deleteById(id);
    }
}
