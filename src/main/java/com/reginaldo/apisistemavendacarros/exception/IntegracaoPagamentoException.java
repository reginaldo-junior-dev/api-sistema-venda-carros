package com.reginaldo.apisistemavendacarros.exception;

// RuntimeException para o @Transactional desfazer o pagamento quando a Stripe falhar
public class IntegracaoPagamentoException extends RuntimeException {
    public IntegracaoPagamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
