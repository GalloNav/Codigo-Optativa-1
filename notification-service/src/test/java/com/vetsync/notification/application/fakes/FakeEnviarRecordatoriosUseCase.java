package com.vetsync.notification.application.fakes;

import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.domain.Notificacion;
import java.util.List;

/** Fake de {@link EnviarRecordatoriosUseCase} para probar adaptadores de entrada. */
public class FakeEnviarRecordatoriosUseCase implements EnviarRecordatoriosUseCase {

    private List<Notificacion> respuesta = List.of();
    private RuntimeException fallo;
    private int invocaciones;

    @Override
    public List<Notificacion> enviarRecordatorios() {
        invocaciones++;
        if (fallo != null) {
            throw fallo;
        }
        return respuesta;
    }

    public void responderCon(List<Notificacion> notificaciones) {
        respuesta = notificaciones;
    }

    public void fallarCon(RuntimeException error) {
        fallo = error;
    }

    /** Cuántas veces se invocó el caso de uso. */
    public int invocaciones() {
        return invocaciones;
    }
}
