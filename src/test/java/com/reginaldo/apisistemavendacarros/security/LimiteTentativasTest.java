package com.reginaldo.apisistemavendacarros.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LimiteTentativasTest {

    // Relógio que o teste avança à mão, para não esperar 15 minutos de verdade
    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-10-06T12:00:00Z");

        void avancar(Duration tempo) { agora = agora.plus(tempo); }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final LimiteTentativas limite = new LimiteTentativas(3, Duration.ofMinutes(15), relogio);

    @Test
    void bloqueiaAoAtingirOMaximoDeFalhas() {
        limite.registrarFalha("login:ana");
        limite.registrarFalha("login:ana");
        assertThat(limite.bloqueado("login:ana")).isFalse();

        limite.registrarFalha("login:ana");
        assertThat(limite.bloqueado("login:ana")).isTrue();
        assertThat(limite.bloqueado("login:bruno")).isFalse();
    }

    @Test
    void desbloqueiaQuandoAJanelaPassa() {
        for (int i = 0; i < 3; i++) limite.registrarFalha("login:ana");

        relogio.avancar(Duration.ofMinutes(16));

        assertThat(limite.bloqueado("login:ana")).isFalse();
        // A contagem recomeça do zero
        limite.registrarFalha("login:ana");
        assertThat(limite.bloqueado("login:ana")).isFalse();
    }

    @Test
    void limparZeraAsFalhas() {
        for (int i = 0; i < 3; i++) limite.registrarFalha("login:ana");

        limite.limpar("login:ana");

        assertThat(limite.bloqueado("login:ana")).isFalse();
    }
}
