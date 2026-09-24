package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.CarroFiltro;
import com.reginaldo.apisistemavendacarros.dto.CarroRequest;
import com.reginaldo.apisistemavendacarros.dto.CarroResponse;
import com.reginaldo.apisistemavendacarros.service.CarroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/carro")
@RequiredArgsConstructor
public class CarroController {

    private final CarroService carroService;

    @PostMapping
    public ResponseEntity<CarroResponse> cadastro (@Valid @RequestBody CarroRequest request) {
        CarroResponse carroResponse = carroService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(carroResponse);
    }

    @GetMapping
    public ResponseEntity<Page<CarroResponse>> lista(@ModelAttribute CarroFiltro filtro,Pageable pageable) {

        Page<CarroResponse> carroResponse = carroService.listar(filtro, pageable);
        return ResponseEntity.ok(carroResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarroResponse> buscarPorId (@PathVariable UUID id) {
        CarroResponse carroResponse = carroService.buscarPorId(id);
        return ResponseEntity.ok(carroResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarroResponse> atualizar (
            @PathVariable UUID id,
            @Valid @RequestBody CarroRequest request) {

        CarroResponse carroResponse = carroService.atualizar(id, request);
        return ResponseEntity.ok(carroResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        carroService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
