package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ParcelaRequest;
import com.reginaldo.apisistemavendacarros.dto.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.ParcelaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParcelaController {

    private final ParcelaService parcelaService;

    @PostMapping("/pagamento/{pagamentoId}/parcelas")
    public ResponseEntity<List<ParcelaResponse>> criarParcelas (@PathVariable UUID pagamentoId, @Valid @RequestBody ParcelaRequest request) {
        List<ParcelaResponse> parcelaResponses = parcelaService.criarParcelas(pagamentoId, request.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaResponses);
    }

    @GetMapping("/pagamento/{pagamentoId}/parcelas")
    public ResponseEntity<List<ParcelaResponse>> listarPorPagamento (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID pagamentoId) {
        List<ParcelaResponse> parcelaResponses = parcelaService.listarPorPagamento(usuario, pagamentoId);
        return ResponseEntity.ok(parcelaResponses);
    }

    @GetMapping("/parcela/{id}")
    public ResponseEntity<ParcelaResponse> buscarPorId (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.buscarPorId(usuario, id);
        return ResponseEntity.ok(parcelaResponse);
    }

    @PutMapping("/parcela/{id}/pagar")
    public ResponseEntity<ParcelaResponse> pagar (@PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.pagar(id);
        return ResponseEntity.ok(parcelaResponse);
    }

    @PutMapping("/parcela/{id}/cancelar")
    public ResponseEntity<ParcelaResponse> cancelar (@PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.cancelar(id);
        return ResponseEntity.ok(parcelaResponse);
    }
}
