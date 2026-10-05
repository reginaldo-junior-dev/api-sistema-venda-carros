package com.reginaldo.apisistemavendacarros.event;

import com.reginaldo.apisistemavendacarros.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Só envia se a operação foi gravada (commit); assíncrono para não travar a requisição/job
@Component
@RequiredArgsConstructor
public class EmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCadastrarUsuario (UsuarioCadastradoEvent evento) {
        emailService.enviarBoasVindas(evento.email(), evento.nome());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAprovarCompra (CompraAprovadaEvent evento) {
        emailService.enviarCompraAprovada(evento.email(), evento.nome(), evento.carro(), evento.valor());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoExpirarCompra (CompraExpiradaEvent evento) {
        emailService.enviarCompraExpirada(evento.email(), evento.nome(), evento.carro(), evento.valor());
    }
}
