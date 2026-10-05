package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.endereco.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.endereco.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/endereco")
@RequiredArgsConstructor
@Tag(name = "Endereços")
public class EnderecoController {

    private final EnderecoService enderecoService;

    @PostMapping
    @Operation(summary = "Cadastrar endereço")
    public ResponseEntity<EnderecoResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody EnderecoRequest request) {
        EnderecoResponse enderecoResponse = enderecoService.cadastro(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoResponse);
    }

    @GetMapping
    @Operation(summary = "Listar meus endereços")
    public ResponseEntity<List<EnderecoResponse>> lista (@AuthenticationPrincipal Usuario usuario) {
        List<EnderecoResponse> enderecoResponses = enderecoService.listarPorUsuario(usuario.getId());
        return ResponseEntity.ok(enderecoResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar meu endereço por id")
    public ResponseEntity<EnderecoResponse> buscarPorId (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        EnderecoResponse enderecoResponse = enderecoService.buscarPorId(usuario.getId(), id);
        return ResponseEntity.ok(enderecoResponse);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<EnderecoResponse> atualizar (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id, @Valid @RequestBody EnderecoRequest request) {
        EnderecoResponse enderecoResponse = enderecoService.atualizar(usuario.getId(), id, request);
        return ResponseEntity.ok(enderecoResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir endereço")
    public ResponseEntity<Void> excluir (@AuthenticationPrincipal Usuario usuario, @PathVariable UUID id) {
        enderecoService.excluir(usuario.getId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
