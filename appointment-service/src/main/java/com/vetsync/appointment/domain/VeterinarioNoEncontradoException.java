package com.vetsync.appointment.domain;

/** El veterinario indicado no está registrado. */
public class VeterinarioNoEncontradoException extends RuntimeException {

    public VeterinarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
