package com.vetsync.notification.application.port.out;

/** Fallo técnico al acceder a la base de datos o a otro servicio detrás de un puerto de salida. */
public class AccesoADatosException extends RuntimeException {

    public AccesoADatosException(String mensaje) {
        super(mensaje);
    }

    public AccesoADatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
