package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.dto.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.ClienteService;
import com.reginaldo.apisistemavendacarros.service.EnderecoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cliente")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final EnderecoService enderecoService;

    @PostMapping
    public ResponseEntity<ClienteResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.cadastro(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<ClienteResponse> buscarClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        ClienteResponse clienteResponse = clienteService.buscarPorUsuario(usuario.getId());
        return ResponseEntity.ok(clienteResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<ClienteResponse> atualizarClienteLogado (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.atualizarPorUsuario(usuario.getId(), request);
        return ResponseEntity.ok(clienteResponse);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        clienteService.excluirPorUsuario(usuario.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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

    @GetMapping("/{id}/enderecos")
    public ResponseEntity<List<EnderecoResponse>> listarEnderecos (@PathVariable UUID id) {
        List<EnderecoResponse> enderecoResponses = enderecoService.listarPorClienteId(id);
        return ResponseEntity.ok(enderecoResponses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        clienteService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
