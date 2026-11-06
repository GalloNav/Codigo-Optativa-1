package com.vetsync.notification.domain;

/** La cita ya tiene registrado su recordatorio (por ejemplo, porque otra instancia se adelantó). */
public class RecordatorioYaEnviadoException extends RuntimeException {

    public RecordatorioYaEnviadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
