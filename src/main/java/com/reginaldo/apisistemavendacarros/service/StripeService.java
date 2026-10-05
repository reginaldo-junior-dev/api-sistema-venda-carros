package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.pagamento.PagamentoCartaoRequest;
import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.IntegracaoPagamentoException;
import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class StripeService {

    private static final Set<String> STATUS_CANCELAVEIS = Set.of(
            "requires_payment_method", "requires_confirmation", "requires_action", "requires_capture");

    private final StripeClient stripeClient;
    private final String webhookSecret;

    public StripeService(StripeClient stripeClient, @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.stripeClient = stripeClient;
        this.webhookSecret = webhookSecret;
    }

    // Cartão recusado não é erro de integração: devolve o PaymentIntent para gravar o pagamento como RECUSADO
    public PaymentIntent criarPagamento(Compra compra, Pagamento pagamento, PagamentoCartaoRequest request) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(emCentavos(compra.getValorTotal()))
                .setCurrency("brl")
                .addAllowedPaymentMethodType(PaymentIntentCreateParams.AllowedPaymentMethodType.CARD)
                .setPaymentMethod(request.paymentMethodId())
                .setConfirm(true)
                .setReceiptEmail(compra.getCliente().getUsuario().getEmail())
                .putMetadata("compraId", compra.getId().toString())
                .putMetadata("pagamentoId", pagamento.getId().toString())
                .build();

        // Requisição reenviada devolve o mesmo PaymentIntent em vez de cobrar de novo
        RequestOptions opcoes = RequestOptions.builder()
                .setIdempotencyKey(pagamento.getId().toString())
                .build();

        try {
            return stripeClient.v1().paymentIntents().create(params, opcoes);
        } catch (CardException e) {
            log.info("Cartão recusado pela Stripe: {} ({})", e.getCode(), e.getDeclineCode());
            PaymentIntent recusado = e.getStripeError().getPaymentIntent();
            if (recusado == null) {
                throw erroApi("Stripe recusou a criação do pagamento", e);
            }
            return recusado;
        } catch (StripeException e) {
            throw erroApi("Stripe recusou a criação do pagamento", e);
        }
    }

    // Vazio quando o PaymentIntent não existe na conta (ex.: evento de teste)
    public Optional<PaymentIntent> buscarPagamento(String paymentIntentId) {
        try {
            return Optional.of(stripeClient.v1().paymentIntents().retrieve(paymentIntentId));
        } catch (InvalidRequestException e) {
            if (e.getStatusCode() != null && e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw erroApi("Falha ao consultar o pagamento " + paymentIntentId + " na Stripe", e);
        } catch (StripeException e) {
            throw erroApi("Falha ao comunicar com a Stripe", e);
        }
    }

    // Impede que um PaymentIntent em aberto seja concluído depois de cancelado aqui.
    // Se já foi pago ou está processando, o webhook traz o resultado
    public void garantirCancelamento(String paymentIntentId) {
        Optional<PaymentIntent> atual = buscarPagamento(paymentIntentId);
        if (atual.isEmpty() || "canceled".equals(atual.get().getStatus())) {
            return;
        }

        if (!STATUS_CANCELAVEIS.contains(atual.get().getStatus())) {
            throw new ConflitoException("Pagamento já foi processado pela Stripe (status "
                    + atual.get().getStatus() + "). Aguarde a confirmação");
        }

        try {
            stripeClient.v1().paymentIntents().cancel(paymentIntentId);
        } catch (StripeException e) {
            // Ex.: o cliente concluiu o 3D Secure no mesmo instante
            throw erroApi("Falha ao cancelar o pagamento " + paymentIntentId + " na Stripe", e);
        }
    }

    // Usado pelo front em stripe.handleNextAction() para abrir o 3D Secure
    public Optional<String> clientSecretParaAcao(PaymentIntent paymentIntent) {
        if ("requires_action".equals(paymentIntent.getStatus())) {
            return Optional.ofNullable(paymentIntent.getClientSecret());
        }
        return Optional.empty();
    }

    // Confere o HMAC do corpo bruto e rejeita eventos com mais de 5 minutos (replay)
    public Optional<Event> validarEvento(String payload, String assinatura) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook recebido, mas stripe.webhook-secret não está configurado");
            return Optional.empty();
        }

        if (assinatura == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Webhook.constructEvent(payload, assinatura, webhookSecret));
        } catch (SignatureVerificationException e) {
            return Optional.empty();
        }
    }

    // Se a versão da API do evento for diferente da do SDK, getObject() vem vazio; só o id é usado
    public Optional<String> paymentIntentIdDoEvento(Event evento) {
        try {
            var deserializador = evento.getDataObjectDeserializer();
            StripeObject objeto = deserializador.getObject().isPresent()
                    ? deserializador.getObject().get()
                    : deserializador.deserializeUnsafe();
            if (objeto instanceof PaymentIntent paymentIntent) {
                return Optional.of(paymentIntent.getId());
            }
            return Optional.empty();
        } catch (EventDataObjectDeserializationException e) {
            log.warn("Webhook: não foi possível ler o objeto do evento {}", evento.getId(), e);
            return Optional.empty();
        }
    }

    public StatusPagamento converterStatus(PaymentIntent paymentIntent) {
        if (paymentIntent.getStatus() == null) {
            return StatusPagamento.PENDENTE;
        }

        return switch (paymentIntent.getStatus()) {
            case "succeeded" -> StatusPagamento.APROVADO;
            // Volta a esse status depois de uma tentativa que falhou
            case "requires_payment_method" -> paymentIntent.getLastPaymentError() != null
                    ? StatusPagamento.RECUSADO
                    : StatusPagamento.PENDENTE;
            case "canceled" -> StatusPagamento.CANCELADO;
            // processing, requires_action (3D Secure) etc.: o resultado final chega pelo webhook
            default -> StatusPagamento.PENDENTE;
        };
    }

    // A Stripe trabalha em centavos
    private long emCentavos(BigDecimal valor) {
        return valor.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private IntegracaoPagamentoException erroApi(String mensagem, StripeException e) {
        log.error("{} - status {} código {}: {}", mensagem, e.getStatusCode(), e.getCode(), e.getMessage());
        return new IntegracaoPagamentoException(mensagem, e);
    }
}
