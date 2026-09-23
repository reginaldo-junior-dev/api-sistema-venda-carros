package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ParcelaRequest;
import com.reginaldo.apisistemavendacarros.dto.ParcelaResponse;
import com.reginaldo.apisistemavendacarros.service.ParcelaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parcela")
@RequiredArgsConstructor
public class ParcelaController {

    private final ParcelaService parcelaService;

    @PostMapping
    public ResponseEntity<ParcelaResponse> cadastro (@RequestBody ParcelaRequest request) {
        ParcelaResponse parcelaResponse = parcelaService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(parcelaResponse);
    }

    @GetMapping
    public ResponseEntity<List<ParcelaResponse>> lista() {
        List<ParcelaResponse> parcelaResponses = parcelaService.listar();
        return ResponseEntity.ok(parcelaResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParcelaResponse> buscarPorId (@PathVariable UUID id) {
        ParcelaResponse parcelaResponse = parcelaService.buscarPorId(id);
        return ResponseEntity.ok(parcelaResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParcelaResponse> atualizar (@PathVariable UUID id, @RequestBody ParcelaRequest request) {
        ParcelaResponse parcelaResponse = parcelaService.atualizar(id, request);
        return ResponseEntity.ok(parcelaResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        parcelaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
