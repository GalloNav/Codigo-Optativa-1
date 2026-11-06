package com.vetsync.notification.domain;

/** El límite pedido para un listado no es válido: debe estar entre 1 y el máximo permitido. */
public class LimiteDeListadoInvalidoException extends RuntimeException {

    public LimiteDeListadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
