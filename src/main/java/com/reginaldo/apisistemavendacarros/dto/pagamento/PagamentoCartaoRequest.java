package com.reginaldo.apisistemavendacarros.dto.pagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// paymentMethodId (pm_...) é gerado pelo Stripe.js: o número do cartão nunca passa pela API
public record PagamentoCartaoRequest(
        @NotNull(message = "Compra é obrigatória")
        UUID compraId,

        @NotBlank(message = "Método de pagamento da Stripe é obrigatório")
        String paymentMethodId
) {
}
