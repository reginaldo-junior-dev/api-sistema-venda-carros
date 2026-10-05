package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.interesse.InteresseCarroRequest;
import com.reginaldo.apisistemavendacarros.dto.interesse.InteresseCarroResponse;
import com.reginaldo.apisistemavendacarros.dto.interesse.InteresseCarroStatusRequest;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.InteresseCarroService;
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
@Tag(name = "Interesses")
public class InteresseCarroController {

    private final InteresseCarroService interesseCarroService;

    @PostMapping("/carro/{carroId}/interesse")
    @Operation(summary = "Demonstrar interesse em um carro")
    public ResponseEntity<InteresseCarroResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID carroId, @Valid @RequestBody InteresseCarroRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.cadastro(usuario.getId(), carroId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(interesseCarroResponse);
    }

    @GetMapping("/cliente/me/interesses")
    @Operation(summary = "Listar meus interesses")
    public ResponseEntity<Page<InteresseCarroResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario, @ParameterObject @PageableDefault(size = 20, sort = "dataInteresse", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InteresseCarroResponse> interesseCarroResponses = interesseCarroService.listarPorUsuario(usuario.getId(), pageable);
        return ResponseEntity.ok(interesseCarroResponses);
    }

    @GetMapping("/interesse")
    @Operation(summary = "[Admin] Listar interesses")
    public ResponseEntity<Page<InteresseCarroResponse>> lista(@ParameterObject @PageableDefault(size = 20, sort = "dataInteresse", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InteresseCarroResponse> interesseCarroResponses = interesseCarroService.listar(pageable);
        return ResponseEntity.ok(interesseCarroResponses);
    }

    @GetMapping("/interesse/{id}")
    @Operation(summary = "[Admin] Buscar interesse por id")
    public ResponseEntity<InteresseCarroResponse> buscarPorId (@PathVariable UUID id) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.buscarPorId(id);
        return ResponseEntity.ok(interesseCarroResponse);
    }

    @PutMapping("/interesse/{id}/status")
    @Operation(summary = "[Admin] Atualizar status do interesse")
    public ResponseEntity<InteresseCarroResponse> atualizarStatus (@PathVariable UUID id, @Valid @RequestBody InteresseCarroStatusRequest request) {
        InteresseCarroResponse interesseCarroResponse = interesseCarroService.atualizarStatus(id, request.status());
        return ResponseEntity.ok(interesseCarroResponse);
    }
}
