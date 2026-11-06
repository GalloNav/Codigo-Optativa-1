package com.vetsync.appointment.domain;

/** El rango de fechas consultado no es válido: el inicio es posterior al fin o el rango es demasiado largo. */
public class RangoDeFechasInvalidoException extends RuntimeException {

    public RangoDeFechasInvalidoException(String mensaje) {
        super(mensaje);
    }
}
