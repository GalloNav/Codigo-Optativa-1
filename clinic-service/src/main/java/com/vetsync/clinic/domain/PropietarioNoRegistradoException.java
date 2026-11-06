package com.vetsync.clinic.domain;

/** El propietario indicado no existe. */
public class PropietarioNoRegistradoException extends RuntimeException {

    public PropietarioNoRegistradoException(String mensaje) {
        super(mensaje);
    }
}
