package com.vetsync.notification.application.fakes;

import com.vetsync.notification.application.port.in.ListarNotificacionesUseCase;
import com.vetsync.notification.domain.Notificacion;
import java.util.List;

/** Fake de {@link ListarNotificacionesUseCase} para probar adaptadores de entrada. */
public class FakeListarNotificacionesUseCase implements ListarNotificacionesUseCase {

    private List<Notificacion> respuesta = List.of();
    private RuntimeException fallo;
    private Integer ultimoLimite;

    @Override
    public List<Notificacion> listarNotificaciones(int limite) {
        ultimoLimite = limite;
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

    /** El límite de la última invocación, o {@code null} si el caso de uso no se invocó. */
    public Integer ultimoLimite() {
        return ultimoLimite;
    }

    public void reiniciar() {
        respuesta = List.of();
        fallo = null;
        ultimoLimite = null;
    }
}
