package com.vetsync.clinic.application.port.out;

/** Fallo técnico al acceder a la base de datos detrás de un puerto de salida. */
public class AccesoADatosException extends RuntimeException {

    public AccesoADatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
