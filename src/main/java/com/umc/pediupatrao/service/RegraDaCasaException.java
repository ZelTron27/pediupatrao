package com.umc.pediupatrao.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class RegraDaCasaException extends RuntimeException {

    public RegraDaCasaException(String mensagem) {
        super(mensagem);
    }
}
