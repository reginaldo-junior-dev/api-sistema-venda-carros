package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.compra.CompraRequest;
import com.reginaldo.apisistemavendacarros.dto.compra.CompraResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.CompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Compras")
public class CompraController {

    private final CompraService compraService;

    @PostMapping("/compra")
    @Operation(summary = "Comprar carro (reserva o carro)")
    public ResponseEntity<CompraResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody CompraRequest request) {
        CompraResponse compraResponse = compraService.cadastro(usuario.getId(), request.carroId());
        return ResponseEntity.status(HttpStatus.CREATED).body(compraResponse);
    }

    @GetMapping("/cliente/me/compras")
    @Operation(summary = "Listar minhas compras")
    public ResponseEntity<Page<CompraResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario, @ParameterObject @PageableDefault(size = 20, sort = "dataCompra", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CompraResponse> compraResponses = compraService.listarPorUsuario(usuario.getId(), pageable);
        return ResponseEntity.ok(compraResponses);
    }

    @GetMapping("/compra")
    @Operation(summary = "[Admin] Listar compras")
    public ResponseEntity<Page<CompraResponse>> lista(@ParameterObject @PageableDefault(size = 20, sort = "dataCompra", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CompraResponse> compraResponses = compraService.listar(pageable);
        return ResponseEntity.ok(compraResponses);
    }

    @GetMapping("/compra/{id}")
    @Operation(summary = "[Admin] Buscar compra por id")
    public ResponseEntity<CompraResponse> buscarPorId (@PathVariable UUID id) {
        CompraResponse compraResponse = compraService.buscarPorId(id);
        return ResponseEntity.ok(compraResponse);
    }

    @PutMapping("/compra/{id}/cancelar")
    @Operation(summary = "[Admin] Cancelar compra e liberar o carro")
    public ResponseEntity<CompraResponse> cancelar (@PathVariable UUID id) {
        CompraResponse compraResponse = compraService.cancelar(id);
        return ResponseEntity.ok(compraResponse);
    }
}
