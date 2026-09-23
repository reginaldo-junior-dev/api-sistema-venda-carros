package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResponse> cadastro (@RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponse);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> lista() {
        List<ClienteResponse> clienteResponses = clienteService.listar();
        return ResponseEntity.ok(clienteResponses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> buscarPorId (@PathVariable UUID id) {
        ClienteResponse clienteResponse = clienteService.buscarPorId(id);
        return ResponseEntity.ok(clienteResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> atualizar (@PathVariable UUID id, @RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.atualizar(id, request);
        return ResponseEntity.ok(clienteResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        clienteService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
