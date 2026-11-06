package com.vetsync.clinic.domain;

/** La mascota indicada no existe. */
public class MascotaNoEncontradaException extends RuntimeException {

    public MascotaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
