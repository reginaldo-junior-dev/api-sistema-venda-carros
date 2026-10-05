package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.cor.CorRequest;
import com.reginaldo.apisistemavendacarros.dto.cor.CorResponse;
import com.reginaldo.apisistemavendacarros.service.CorService;
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
@RequestMapping("/cor")
@RequiredArgsConstructor
@Tag(name = "Cores")
public class CorController {

    private final CorService corService;

    @PostMapping
    @Operation(summary = "[Admin] Cadastrar cor")
    public ResponseEntity<CorResponse> cadastro (@Valid @RequestBody CorRequest request) {
        CorResponse corResponse = corService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(corResponse);
    }

    @GetMapping
    @Operation(summary = "Listar cores")
    @SecurityRequirements
    public ResponseEntity<List<CorResponse>> lista () {
        List<CorResponse> corResponses = corService.listar();
        return ResponseEntity.ok(corResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cor por id")
    @SecurityRequirements
    public ResponseEntity<CorResponse> buscarPorId (@PathVariable UUID id) {
        CorResponse corResponse = corService.buscarPorId(id);
        return ResponseEntity.ok(corResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Atualizar cor")
    public ResponseEntity<CorResponse> atualizar (@PathVariable UUID id, @Valid @RequestBody CorRequest request) {
        CorResponse corResponse = corService.atualizar(id, request);
        return ResponseEntity.ok(corResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir cor")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        corService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
