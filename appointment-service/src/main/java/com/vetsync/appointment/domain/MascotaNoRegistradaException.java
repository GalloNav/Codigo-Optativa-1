package com.vetsync.appointment.domain;

/** La mascota indicada no existe en clinic-service. */
public class MascotaNoRegistradaException extends RuntimeException {

    public MascotaNoRegistradaException(String mensaje) {
        super(mensaje);
    }
}
