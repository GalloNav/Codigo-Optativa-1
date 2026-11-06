package com.vetsync.appointment.application.port.out;

/** Fallo técnico al acceder a un recurso externo (base de datos u otro servicio) detrás de un puerto de salida. */
public class AccesoADatosException extends RuntimeException {

    public AccesoADatosException(String mensaje) {
        super(mensaje);
    }

    public AccesoADatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
