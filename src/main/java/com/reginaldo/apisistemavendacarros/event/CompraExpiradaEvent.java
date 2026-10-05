package com.reginaldo.apisistemavendacarros.event;

import java.math.BigDecimal;

// Publicado quando o job cancela uma compra pendente sem pagamento
public record CompraExpiradaEvent(String email, String nome, String carro, BigDecimal valor) {
}
