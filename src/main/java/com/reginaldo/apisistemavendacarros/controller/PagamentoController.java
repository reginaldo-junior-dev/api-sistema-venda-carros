package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.PagamentoService;
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
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping("/pagamento")
    public ResponseEntity<PagamentoResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody PagamentoRequest request) {
        PagamentoResponse pagamentoResponse = pagamentoService.cadastro(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoResponse);
    }

    @GetMapping("/cliente/me/pagamentos")
    public ResponseEntity<List<PagamentoResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        List<PagamentoResponse> pagamentoResponses = pagamentoService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(pagamentoResponses);
    }

    @GetMapping("/pagamento")
    public ResponseEntity<List<PagamentoResponse>> lista() {
        List<PagamentoResponse> pagamentoResponses = pagamentoService.listar();
        return ResponseEntity.ok(pagamentoResponses);
    }

    @GetMapping("/pagamento/{id}")
    public ResponseEntity<PagamentoResponse> buscarPorId (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/aprovar")
    public ResponseEntity<PagamentoResponse> aprovar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.aprovar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/recusar")
    public ResponseEntity<PagamentoResponse> recusar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.recusar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/cancelar")
    public ResponseEntity<PagamentoResponse> cancelar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.cancelar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }
}
