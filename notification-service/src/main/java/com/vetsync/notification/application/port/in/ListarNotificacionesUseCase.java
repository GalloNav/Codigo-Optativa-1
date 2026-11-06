package com.vetsync.notification.application.port.in;

import com.vetsync.notification.domain.Notificacion;
import java.util.List;

/** Caso de uso: listar los recordatorios enviados más recientes, del más reciente al más antiguo. */
public interface ListarNotificacionesUseCase {

    /**
     * Hasta {@code limite} recordatorios. El límite debe estar entre 1 y el máximo permitido; si no,
     * lanza {@link com.vetsync.notification.domain.LimiteDeListadoInvalidoException}.
     */
    List<Notificacion> listarNotificaciones(int limite);
}
