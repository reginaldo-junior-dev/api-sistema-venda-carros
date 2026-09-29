package com.reginaldo.apisistemavendacarros.exception;

import com.reginaldo.apisistemavendacarros.dto.ErroResposta;
import com.reginaldo.apisistemavendacarros.dto.ErroValidacao;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResposta> erro (ConflitoException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erroResposta);
    }

    // Chega aqui quando duas requisições simultâneas passam pela validação do Service
    // e a constraint do banco (UNIQUE/FK) barra a segunda
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> erro (DataIntegrityViolationException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.CONFLICT.value(),
                "Operação conflita com dados já existentes",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erroResposta);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> erro (HttpMessageNotReadableException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da requisição inválido ou com valor não reconhecido",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResposta);
    }
}
