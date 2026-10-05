package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.service.PagamentoService;
import com.reginaldo.apisistemavendacarros.service.StripeService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@Hidden
@Slf4j
@RestController
@RequestMapping("/stripe")
@RequiredArgsConstructor
public class StripeController {

    private static final Set<String> EVENTOS_TRATADOS = Set.of(
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled"
    );

    private final StripeService stripeService;
    private final PagamentoService pagamentoService;

    // O corpo chega como String bruta para a assinatura bater.
    // Qualquer resposta diferente de 2xx faz a Stripe reenviar o evento
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook (
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String assinatura) {

        Optional<Event> evento = stripeService.validarEvento(payload, assinatura);
        if (evento.isEmpty()) {
            log.warn("Webhook da Stripe com assinatura inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!EVENTOS_TRATADOS.contains(evento.get().getType())) {
            return ResponseEntity.ok().build();
        }

        Optional<String> paymentIntentId = stripeService.paymentIntentIdDoEvento(evento.get());
        if (paymentIntentId.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        // Eventos podem chegar fora de ordem, por isso o status é consultado na API
        Optional<PaymentIntent> paymentIntent = stripeService.buscarPagamento(paymentIntentId.get());

        // 200 para a Stripe não reenviar um evento que nunca vai ser processado
        if (paymentIntent.isEmpty()) {
            log.warn("Webhook: PaymentIntent {} não encontrado na Stripe", paymentIntentId.get());
            return ResponseEntity.ok().build();
        }

        pagamentoService.atualizarPeloPaymentIntent(paymentIntent.get());

        return ResponseEntity.ok().build();
    }
}
