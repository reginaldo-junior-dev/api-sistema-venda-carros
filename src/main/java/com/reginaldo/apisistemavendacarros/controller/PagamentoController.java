package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pagamento")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    public ResponseEntity<PagamentoResponse> cadastro (@RequestBody PagamentoRequest request) {
        PagamentoResponse pagamentoResponse = pagamentoService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoResponse);
    }

    @GetMapping
    public ResponseEntity<List<PagamentoResponse>> lista() {
        List<PagamentoResponse> pagamentoResponses = pagamentoService.listar();
        return ResponseEntity.ok(pagamentoResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponse> buscarPorId (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PagamentoResponse> atualizar (@PathVariable UUID id, @RequestBody PagamentoRequest request) {
        PagamentoResponse pagamentoResponse = pagamentoService.atualizar(id, request);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        pagamentoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
