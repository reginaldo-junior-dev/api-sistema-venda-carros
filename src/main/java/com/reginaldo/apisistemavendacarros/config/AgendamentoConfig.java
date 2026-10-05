package com.reginaldo.apisistemavendacarros.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Desligado nos testes (agendamento.ativo=false)
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "agendamento.ativo", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfig {
}
