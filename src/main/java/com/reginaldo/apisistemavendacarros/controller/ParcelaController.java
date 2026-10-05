package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.parcela.ParcelaRequest;
import com.reginaldo.apisistemavendacarros.dto.parcela.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.ParcelaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Parcelas")
public class ParcelaController {

    private final ParcelaService parcelaService;

    @PostMapping("/pagamento/{pagamentoId}/parcelas")
    @Operation(summary = "[Admin] Gerar parcelas de um pagamento")
    public ResponseEntity<List<ParcelaResponse>> criarParcelas (@PathVariable UUID pagamentoId, @Valid @RequestBody ParcelaRequest request) {
        List<ParcelaResponse> parcelaResponses = parcelaService.criarParcelas(pagamentoId, request.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaResponses);
    }

    @GetMapping("/pagamento/{pagamentoId}/parcelas")
    @Operation(summary = "Listar parcelas de um pagamento (admin ou dono)")
    public ResponseEntity<List<ParcelaResponse>> listarPorPagamento (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID pagamentoId) {
        List<ParcelaResponse> parcelaResponses = parcelaService.listarPorPagamento(usuario, pagamentoId);
        return ResponseEntity.ok(parcelaResponses);
    }

    @GetMapping("/parcela/{id}")
    @Operation(summary = "Buscar parcela por id (admin ou dono)")
    public ResponseEntity<ParcelaResponse> buscarPorId (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.buscarPorId(usuario, id);
        return ResponseEntity.ok(parcelaResponse);
    }

    @PutMapping("/parcela/{id}/pagar")
    @Operation(summary = "[Admin] Marcar parcela como paga")
    public ResponseEntity<ParcelaResponse> pagar (@PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.pagar(id);
        return ResponseEntity.ok(parcelaResponse);
    }

    @PutMapping("/parcela/{id}/cancelar")
    @Operation(summary = "[Admin] Cancelar parcela")
    public ResponseEntity<ParcelaResponse> cancelar (@PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.cancelar(id);
        return ResponseEntity.ok(parcelaResponse);
    }
}
