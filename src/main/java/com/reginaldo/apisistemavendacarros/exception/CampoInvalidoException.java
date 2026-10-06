package com.reginaldo.apisistemavendacarros.exception;

import lombok.Getter;

// Erro de regra ligado a um campo do formulário: volta no mesmo formato da validação ({ mensagens: { campo: msg } })
@Getter
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}
