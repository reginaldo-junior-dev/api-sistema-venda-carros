package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.CompraService;
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
public class CompraController {

    private final CompraService compraService;

    @PostMapping("/compra")
    public ResponseEntity<CompraResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody CompraRequest request) {
        CompraResponse compraResponse = compraService.cadastro(usuario.getId(), request.carroId());
        return ResponseEntity.status(HttpStatus.CREATED).body(compraResponse);
    }

    @GetMapping("/cliente/me/compras")
    public ResponseEntity<List<CompraResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        List<CompraResponse> compraResponses = compraService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(compraResponses);
    }

    @GetMapping("/compra")
    public ResponseEntity<List<CompraResponse>> lista() {
        List<CompraResponse> compraResponses = compraService.listar();
        return ResponseEntity.ok(compraResponses);
    }

    @GetMapping("/compra/{id}")
    public ResponseEntity<CompraResponse> buscarPorId (@PathVariable UUID id) {
        CompraResponse compraResponse = compraService.buscarPorId(id);
        return ResponseEntity.ok(compraResponse);
    }

    @PutMapping("/compra/{id}/cancelar")
    public ResponseEntity<CompraResponse> cancelar (@PathVariable UUID id) {
        CompraResponse compraResponse = compraService.cancelar(id);
        return ResponseEntity.ok(compraResponse);
    }
}
