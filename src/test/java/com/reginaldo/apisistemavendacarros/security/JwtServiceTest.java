package com.reginaldo.apisistemavendacarros.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static JwtService comSegredo(String segredo) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", segredo);
        return service;
    }

    @Test
    void recusaSubirComSegredoCurto() {
        assertThatThrownBy(() -> comSegredo("123").validarSegredo())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pelo menos 32 caracteres");
    }

    @Test
    void aceitaSegredoComTrintaEDoisCaracteres() {
        assertThatCode(() -> comSegredo("a".repeat(32)).validarSegredo()).doesNotThrowAnyException();
    }
}
