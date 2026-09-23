package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.EnderecoRequest;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.service.EnderecoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/endereco")
@RequiredArgsConstructor
public class EnderecoController {

    private final EnderecoService enderecoService;

    @PostMapping
    public ResponseEntity<EnderecoResponse> cadastro (@RequestBody EnderecoRequest request) {
        EnderecoResponse enderecoResponse = enderecoService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoResponse);
    }

    @GetMapping
    public ResponseEntity<List<EnderecoResponse>> lista() {
        List<EnderecoResponse> enderecoResponses = enderecoService.listar();
        return ResponseEntity.ok(enderecoResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnderecoResponse> buscarPorId (@PathVariable UUID id) {
        EnderecoResponse enderecoResponse = enderecoService.buscarPorId(id);
        return ResponseEntity.ok(enderecoResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnderecoResponse> atualizar (@PathVariable UUID id, @RequestBody EnderecoRequest request) {
        EnderecoResponse enderecoResponse = enderecoService.atualizar(id, request);
        return ResponseEntity.ok(enderecoResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        enderecoService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
