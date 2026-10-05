package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.cliente.ClienteResponse;
import com.reginaldo.apisistemavendacarros.dto.endereco.EnderecoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.ClienteService;
import com.reginaldo.apisistemavendacarros.service.EnderecoService;
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
@RequestMapping("/cliente")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final EnderecoService enderecoService;

    @PostMapping
    @Operation(summary = "Completar cadastro de cliente")
    public ResponseEntity<ClienteResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.cadastro(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Ver meus dados de cliente")
    public ResponseEntity<ClienteResponse> buscarClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        ClienteResponse clienteResponse = clienteService.buscarPorUsuario(usuario.getId());
        return ResponseEntity.ok(clienteResponse);
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar meus dados de cliente")
    public ResponseEntity<ClienteResponse> atualizarClienteLogado (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody ClienteRequest request) {
        ClienteResponse clienteResponse = clienteService.atualizarPorUsuario(usuario.getId(), request);
        return ResponseEntity.ok(clienteResponse);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Excluir meu cadastro de cliente")
    public ResponseEntity<Void> excluirClienteLogado (@AuthenticationPrincipal Usuario usuario) {
        clienteService.excluirPorUsuario(usuario.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "[Admin] Listar clientes")
    public ResponseEntity<Page<ClienteResponse>> lista(@ParameterObject @PageableDefault(size = 20, sort = "usuario.nomeCompleto", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<ClienteResponse> clienteResponses = clienteService.listar(pageable);
        return ResponseEntity.ok(clienteResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin] Buscar cliente por id")
    public ResponseEntity<ClienteResponse> buscarPorId (@PathVariable UUID id) {
        ClienteResponse clienteResponse = clienteService.buscarPorId(id);
        return ResponseEntity.ok(clienteResponse);
    }

    @GetMapping("/{id}/enderecos")
    @Operation(summary = "[Admin] Listar endereços de um cliente")
    public ResponseEntity<List<EnderecoResponse>> listarEnderecos (@PathVariable UUID id) {
        List<EnderecoResponse> enderecoResponses = enderecoService.listarPorClienteId(id);
        return ResponseEntity.ok(enderecoResponses);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir cliente")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        clienteService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
