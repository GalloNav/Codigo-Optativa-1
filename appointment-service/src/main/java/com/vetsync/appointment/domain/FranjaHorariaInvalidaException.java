package com.vetsync.appointment.domain;

/** La franja solicitada no es válida para agendar (por ejemplo, no es posterior a la hora actual). */
public class FranjaHorariaInvalidaException extends RuntimeException {

    public FranjaHorariaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
