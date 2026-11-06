package com.vetsync.clinic.domain;

/** Ya existe un propietario con ese correo. */
public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException(String mensaje) {
        super(mensaje);
    }

    public CorreoYaRegistradoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
