package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.RegraDaCasaException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = RestController.class)
public class TratamentoErrosApi {

    @ExceptionHandler(RegraDaCasaException.class)
    public ResponseEntity<Map<String, String>> regraViolada(RegraDaCasaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(BindException e) {
        String campo = e.getFieldError() != null ? e.getFieldError().getField() : "consulta";
        return resposta(HttpStatus.BAD_REQUEST, "O parâmetro " + campo + " recebeu um valor que não foi reconhecido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST, "O parâmetro " + e.getName() + " recebeu um valor que não foi reconhecido.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(IllegalArgumentException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> acessoNegado(AccessDeniedException e) {
        return resposta(HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para esta operação.");
    }

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
