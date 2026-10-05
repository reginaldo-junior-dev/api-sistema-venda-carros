package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.favorito.FavoritoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping("/carro/{carroId}/favorito")
    @Operation(summary = "Favoritar carro")
    public ResponseEntity<FavoritoResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId) {
        FavoritoResponse favoritoResponse = favoritoService.cadastro(usuario.getId(), carroId);
        return ResponseEntity.status(HttpStatus.CREATED).body(favoritoResponse);
    }

    @DeleteMapping("/carro/{carroId}/favorito")
    @Operation(summary = "Remover carro dos favoritos")
    public ResponseEntity<Void> excluir (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId) {
        favoritoService.excluir(usuario.getId(), carroId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/cliente/me/favoritos")
    @Operation(summary = "Listar meus favoritos")
    public ResponseEntity<Page<FavoritoResponse>> listar (@AuthenticationPrincipal Usuario usuario, @ParameterObject @PageableDefault(size = 20, sort = "dataFavorito", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FavoritoResponse> favoritoResponses = favoritoService.listarPorUsuario(usuario.getId(), pageable);
        return ResponseEntity.ok(favoritoResponses);
    }
}
