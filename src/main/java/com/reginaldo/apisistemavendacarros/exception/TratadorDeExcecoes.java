package com.reginaldo.apisistemavendacarros.exception;

import com.reginaldo.apisistemavendacarros.dto.erro.ErroResposta;
import com.reginaldo.apisistemavendacarros.dto.erro.ErroValidacao;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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

    @ExceptionHandler(IntegracaoPagamentoException.class)
    public ResponseEntity<ErroResposta> erro (IntegracaoPagamentoException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.BAD_GATEWAY.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(erroResposta);
    }

    // Requisições simultâneas passam pela validação do service e a constraint do banco barra a segunda
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

    // Limite definido em spring.servlet.multipart.max-file-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> erro (MaxUploadSizeExceededException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.CONTENT_TOO_LARGE.value(),
                "Arquivo maior que o limite permitido de 10 MB",
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(erroResposta);
    }

    // ?sort= com campo que não existe na entidade
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErroResposta> erro (PropertyReferenceException ex) {
        ErroResposta erroResposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                "Campo de ordenação inválido: " + ex.getPropertyName(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResposta);
    }
}
