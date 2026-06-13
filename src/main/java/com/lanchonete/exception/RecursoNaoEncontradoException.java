package com.lanchonete.exception;

import java.util.UUID;

public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }

    public RecursoNaoEncontradoException(String recurso, UUID id) {
        super(String.format("%s com o id %d não encontrado(a)", recurso,id));
    }
}
