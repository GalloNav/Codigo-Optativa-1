package com.vetsync.appointment.domain;

/** El veterinario ya tiene una cita programada en la franja solicitada. */
public class ConflictoDeHorarioException extends RuntimeException {

    public ConflictoDeHorarioException(String mensaje) {
        super(mensaje);
    }

    public ConflictoDeHorarioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
