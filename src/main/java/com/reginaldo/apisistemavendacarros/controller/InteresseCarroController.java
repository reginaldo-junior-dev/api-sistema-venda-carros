package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.dto.InteresseCarroStatusRequest;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.InteresseCarroService;
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
public class InteresseCarroController {

    private final InteresseCarroService interesseCarroService;

    @PostMapping("/carro/{carroId}/interesse")
    public ResponseEntity<InteresseCarroResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId, @Valid @RequestBody InteresseCarroRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.cadastro(usuario.getId(), carroId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(interesseCarroResponse);
    }

    @GetMapping("/cliente/me/interesses")
    public ResponseEntity<List<InteresseCarroResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        List<InteresseCarroResponse> interesseCarroResponses = interesseCarroService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(interesseCarroResponses);
    }

    @GetMapping("/interesse")
    public ResponseEntity<List<InteresseCarroResponse>> lista() {
        List<InteresseCarroResponse> interesseCarroResponses = interesseCarroService.listar();
        return ResponseEntity.ok(interesseCarroResponses);
    }

    @GetMapping("/interesse/{id}")
    public ResponseEntity<InteresseCarroResponse> buscarPorId (@PathVariable UUID id) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.buscarPorId(id);
        return ResponseEntity.ok(interesseCarroResponse);
    }

    @PutMapping("/interesse/{id}/status")
    public ResponseEntity<InteresseCarroResponse> atualizarStatus (@PathVariable UUID id, @Valid @RequestBody InteresseCarroStatusRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.atualizarStatus(id, request.status());
        return ResponseEntity.ok(interesseCarroResponse);
    }
}
