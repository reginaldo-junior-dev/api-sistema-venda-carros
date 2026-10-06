package com.reginaldo.apisistemavendacarros.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia uma chave (ex.: o e-mail do login) depois de muitas falhas seguidas, por um tempo.
 * Impede adivinhar senhas por tentativa e erro. Fica em memória: reiniciar a API zera as contagens.
 */
@Component
public class LimiteTentativas {

    private record Falhas(int quantidade, Instant inicio) {}

    private final int maximo;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();

    // Construtor usado pelo Spring; o outro recebe um relógio controlado nos testes
    @Autowired
    public LimiteTentativas(@Value("${seguranca.tentativas.maximo:5}") int maximo,
                            @Value("${seguranca.tentativas.janela:PT15M}") Duration janela) {
        this(maximo, janela, Clock.systemUTC());
    }

    LimiteTentativas(int maximo, Duration janela, Clock relogio) {
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    public boolean bloqueado(String chave) {
        Falhas atual = falhas.get(chave);
        return atual != null && !expirou(atual) && atual.quantidade() >= maximo;
    }

    public void registrarFalha(String chave) {
        falhas.compute(chave, (k, atual) -> atual == null || expirou(atual)
                ? new Falhas(1, relogio.instant())
                : new Falhas(atual.quantidade() + 1, atual.inicio()));
        // Sem isso, chaves de quem nunca mais tentou ficariam na memória para sempre
        if (falhas.size() > 10_000) {
            falhas.values().removeIf(this::expirou);
        }
    }

    // Acertou: as falhas anteriores deixam de contar
    public void limpar(String chave) {
        falhas.remove(chave);
    }

    private boolean expirou(Falhas registro) {
        return registro.inicio().plus(janela).isBefore(relogio.instant());
    }
}
