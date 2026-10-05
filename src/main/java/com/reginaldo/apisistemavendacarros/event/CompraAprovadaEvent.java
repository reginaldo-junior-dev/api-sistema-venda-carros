package com.reginaldo.apisistemavendacarros.event;

import java.math.BigDecimal;

// Publicado quando o pagamento é aprovado e o carro é vendido
public record CompraAprovadaEvent(String email, String nome, String carro, BigDecimal valor) {
}
