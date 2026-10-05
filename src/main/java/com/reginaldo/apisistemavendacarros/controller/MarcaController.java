package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.marca.MarcaRequest;
import com.reginaldo.apisistemavendacarros.dto.marca.MarcaResponse;
import com.reginaldo.apisistemavendacarros.service.MarcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/marca")
@RequiredArgsConstructor
@Tag(name = "Marcas")
public class MarcaController {

    private final MarcaService marcaService;

    @PostMapping
    @Operation(summary = "[Admin] Cadastrar marca")
    public ResponseEntity<MarcaResponse> cadastro (@Valid @RequestBody MarcaRequest request) {
        MarcaResponse marcaResponse = marcaService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(marcaResponse);
    }

    @GetMapping
    @Operation(summary = "Listar marcas")
    @SecurityRequirements
    public ResponseEntity<List<MarcaResponse>> lista () {
        List<MarcaResponse> marcaResponses = marcaService.lista();
        return ResponseEntity.ok(marcaResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar marca por id")
    @SecurityRequirements
    public ResponseEntity<MarcaResponse> buscarPorId (@PathVariable UUID id) {
        MarcaResponse marcaResponse = marcaService.buscarPorId(id);
        return ResponseEntity.ok(marcaResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Atualizar marca")
    public ResponseEntity<MarcaResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody MarcaRequest request) {
        MarcaResponse marcaResponse = marcaService.atualizar(id, request);
        return ResponseEntity.ok(marcaResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir marca")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        marcaService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
