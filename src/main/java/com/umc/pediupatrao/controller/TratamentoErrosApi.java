package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.service.RegraDaCasaException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class TratamentoErrosApi {

    @ExceptionHandler(RegraDaCasaException.class)
    public ResponseEntity<Map<String, String>> regraViolada(RegraDaCasaException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
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
