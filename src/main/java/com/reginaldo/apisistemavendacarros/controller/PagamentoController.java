package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoCartaoRequest;
import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoRequest;
import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.service.PagamentoService;
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
@Tag(name = "Pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping("/pagamento/cartao")
    @Operation(summary = "Pagar com cartão (Stripe)",
            description = "O paymentMethodId (pm_...) vem do Stripe.js. Em modo de teste: pm_card_visa (aprovado), pm_card_chargeDeclined (recusado), pm_card_authenticationRequired (3D Secure: volta PENDENTE com clientSecret para o front chamar stripe.handleNextAction).")
    public ResponseEntity<PagamentoResponse> pagamentoCartao (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody PagamentoCartaoRequest request) {
        PagamentoResponse pagamentoResponse = pagamentoService.pagamentoCartao(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoResponse);
    }

    @PostMapping("/pagamento")
    @Operation(summary = "Registrar pagamento manual (PIX, boleto...)")
    public ResponseEntity<PagamentoResponse> cadastro (@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody PagamentoRequest request) {
        PagamentoResponse pagamentoResponse = pagamentoService.cadastro(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pagamentoResponse);
    }

    @GetMapping("/cliente/me/pagamentos")
    @Operation(summary = "Listar meus pagamentos")
    public ResponseEntity<Page<PagamentoResponse>> listarDoClienteLogado (@AuthenticationPrincipal Usuario usuario, @ParameterObject @PageableDefault(size = 20, sort = "compra.dataCompra", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PagamentoResponse> pagamentoResponses = pagamentoService.listarPorUsuario(usuario.getId(), pageable);
        return ResponseEntity.ok(pagamentoResponses);
    }

    @GetMapping("/pagamento")
    @Operation(summary = "[Admin] Listar pagamentos")
    public ResponseEntity<Page<PagamentoResponse>> lista(@ParameterObject @PageableDefault(size = 20, sort = "compra.dataCompra", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PagamentoResponse> pagamentoResponses = pagamentoService.listar(pageable);
        return ResponseEntity.ok(pagamentoResponses);
    }

    @GetMapping("/pagamento/{id}")
    @Operation(summary = "[Admin] Buscar pagamento por id")
    public ResponseEntity<PagamentoResponse> buscarPorId (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/aprovar")
    @Operation(summary = "[Admin] Aprovar pagamento manual",
            description = "Pagamentos com cartão não podem ser aprovados manualmente: quem aprova é a Stripe.")
    public ResponseEntity<PagamentoResponse> aprovar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.aprovar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/recusar")
    @Operation(summary = "[Admin] Recusar pagamento")
    public ResponseEntity<PagamentoResponse> recusar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.recusar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }

    @PutMapping("/pagamento/{id}/cancelar")
    @Operation(summary = "[Admin] Cancelar pagamento")
    public ResponseEntity<PagamentoResponse> cancelar (@PathVariable UUID id) {
        PagamentoResponse pagamentoResponse = pagamentoService.cancelar(id);
        return ResponseEntity.ok(pagamentoResponse);
    }
}
