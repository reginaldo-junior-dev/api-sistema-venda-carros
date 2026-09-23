package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.FavoritoRequest;
import com.reginaldo.apisistemavendacarros.dto.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/favorito")
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping
    public ResponseEntity<FavoritoResponse> cadastro (@RequestBody FavoritoRequest request) {
        FavoritoResponse favoritoResponse = favoritoService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoritoResponse);
    }

    @GetMapping
    public ResponseEntity<List<FavoritoResponse>> lista() {
        List<FavoritoResponse> favoritoResponses = favoritoService.listar();
        return ResponseEntity.ok(favoritoResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FavoritoResponse> buscarPorId (@PathVariable UUID id) {
        FavoritoResponse favoritoResponse = favoritoService.buscarPorId(id);
        return ResponseEntity.ok(favoritoResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FavoritoResponse> atualizar (@PathVariable UUID id, @RequestBody FavoritoRequest request) {
        FavoritoResponse favoritoResponse = favoritoService.atualizar(id, request);
        return ResponseEntity.ok(favoritoResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        favoritoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
