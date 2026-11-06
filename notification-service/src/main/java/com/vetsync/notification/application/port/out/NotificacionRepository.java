package com.vetsync.notification.application.port.out;

import com.vetsync.notification.domain.Notificacion;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Puerto de salida hacia la persistencia de notificaciones. */
public interface NotificacionRepository {

    /** Persiste la notificación y la devuelve con su id asignado. Solo se admite una por cita. */
    Notificacion guardar(Notificacion notificacion);

    /** De las citas indicadas, las que ya recibieron su recordatorio; una sola consulta sin importar cuántas sean. */
    Set<Long> citasYaNotificadas(Collection<Long> citaIds);

    /**
     * Hasta {@code limite} notificaciones, de la más reciente a la más antigua
     * (a igual fecha, la última guardada primero).
     */
    List<Notificacion> buscarRecientes(int limite);
}
