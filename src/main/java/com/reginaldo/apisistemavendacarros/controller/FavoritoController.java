package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.FavoritoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping("/carro/{carroId}/favorito")
    public ResponseEntity<FavoritoResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId) {
        FavoritoResponse favoritoResponse = favoritoService.cadastro(usuario.getId(), carroId);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoritoResponse);
    }

    @DeleteMapping("/carro/{carroId}/favorito")
    public ResponseEntity<Void> excluir (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId) {
        favoritoService.excluir(usuario.getId(), carroId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/cliente/me/favoritos")
    public ResponseEntity<List<FavoritoResponse>> listar (@AuthenticationPrincipal Usuario usuario) {
        List<FavoritoResponse> favoritoResponses = favoritoService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(favoritoResponses);
    }
}
