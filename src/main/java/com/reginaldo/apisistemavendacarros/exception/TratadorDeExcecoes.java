package com.reginaldo.apisistemavendacarros.exception;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class TratadorDeExcecoes {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> erro (RecursoNaoEncontradoException ex) {
        ErroResposta erroResposta = new ErroResposta(
            HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erroResposta);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacao> erro (MethodArgumentNotValidException ex) {
            Map<String, String> erros = new HashMap<>();

            for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                erros.put(error.getField(), error.getDefaultMessage());
            }

            ErroValidacao erroValidacao = new ErroValidacao(
                    HttpStatus.BAD_REQUEST.value(),
                    erros,
                    LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroValidacao);
    }

    @ExceptionHandler(ValorInvalidoException.class)
    public ResponseEntity<ErroResposta> erro (ValorInvalidoException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResposta);
    }



}
