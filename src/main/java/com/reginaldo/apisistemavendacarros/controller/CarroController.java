package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.carro.CarroFiltro;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroRequest;
import com.reginaldo.apisistemavendacarros.dto.carro.CarroResponse;
import com.reginaldo.apisistemavendacarros.service.CarroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/carro")
@RequiredArgsConstructor
@Tag(name = "Carros")
public class CarroController {

    private final CarroService carroService;

    @PostMapping
    @Operation(summary = "[Admin] Cadastrar carro")
    public ResponseEntity<CarroResponse> cadastro (@Valid @RequestBody CarroRequest request) {
        CarroResponse carroResponse = carroService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(carroResponse);
    }

    @GetMapping
    @Operation(summary = "Listar carros com filtros e paginação")
    @SecurityRequirements
    public ResponseEntity<Page<CarroResponse>> lista(@ParameterObject @ModelAttribute CarroFiltro filtro, @ParameterObject Pageable pageable) {

        Page<CarroResponse> carroResponse = carroService.listar(filtro, pageable);
        return ResponseEntity.ok(carroResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar carro por id")
    @SecurityRequirements
    public ResponseEntity<CarroResponse> buscarPorId (@PathVariable UUID id) {
        CarroResponse carroResponse = carroService.buscarPorId(id);
        return ResponseEntity.ok(carroResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "[Admin] Atualizar carro")
    public ResponseEntity<CarroResponse> atualizar (
            @PathVariable UUID id,
            @Valid @RequestBody CarroRequest request) {

        CarroResponse carroResponse = carroService.atualizar(id, request);
        return ResponseEntity.ok(carroResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir carro")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        carroService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
