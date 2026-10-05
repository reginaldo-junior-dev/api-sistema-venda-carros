package com.reginaldo.apisistemavendacarros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async (ex.: envio de e-mail sem travar a requisição)
@Configuration
@EnableAsync
public class AsyncConfig {
}
