package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioAtualizacaoRequest;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.mapper.UsuarioMapper;
import com.reginaldo.apisistemavendacarros.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
@RequestMapping("/usuario")
@RequiredArgsConstructor
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    @PostMapping
    @Operation(summary = "Criar conta")
    @SecurityRequirements
    public ResponseEntity<UsuarioResponse> cadastro (@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioResponse = usuarioService.cadastro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "Ver minha conta")
    public ResponseEntity<UsuarioResponse> usuarioLogado (@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioMapper.toResponse(usuario));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar minha conta")
    public ResponseEntity<UsuarioResponse> atualizarUsuarioLogado (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        UsuarioResponse usuarioResponse = usuarioService.atualizar(usuario.getId(), request);
        return ResponseEntity.ok(usuarioResponse);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Excluir minha conta")
    public ResponseEntity<Void> excluirUsuarioLogado (@AuthenticationPrincipal Usuario usuario) {
        usuarioService.excluir(usuario.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    @Operation(summary = "[Admin] Listar usuários")
    public ResponseEntity<Page<UsuarioResponse>> lista(@ParameterObject @PageableDefault(size = 20, sort = "nomeCompleto", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UsuarioResponse> usuarioResponses = usuarioService.listar(pageable);
        return ResponseEntity.ok(usuarioResponses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "[Admin] Buscar usuário por id")
    public ResponseEntity<UsuarioResponse> buscarPorId (@PathVariable UUID id) {
        UsuarioResponse usuarioResponse = usuarioService.buscarPorId(id);
        return ResponseEntity.ok(usuarioResponse);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "[Admin] Excluir usuário")
    public ResponseEntity<Void> excluir (@PathVariable UUID id) {
        usuarioService.excluir(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
