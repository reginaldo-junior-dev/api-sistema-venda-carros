package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastro (@RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioResponse = usuarioService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioResponse);
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> lista() {
        List<UsuarioResponse> usuarioResponses = usuarioService.listar();
        return ResponseEntity.ok(usuarioResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId (@PathVariable UUID id) {
        UsuarioResponse usuarioResponse = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuarioResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar (@PathVariable UUID id, @RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioResponse = usuarioService.atualizar(id, request);
        return ResponseEntity.ok(usuarioResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        usuarioService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
