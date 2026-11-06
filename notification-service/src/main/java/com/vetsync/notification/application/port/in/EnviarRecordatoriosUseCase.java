package com.vetsync.notification.application.port.in;

import com.vetsync.notification.domain.Notificacion;
import java.util.List;

/**
 * Caso de uso: enviar el recordatorio de las citas próximas que aún no lo han recibido.
 * Una cita es próxima si empieza entre ahora y ahora más la anticipación configurada.
 * Es idempotente: una cita ya notificada no vuelve a recibir recordatorio.
 * Devuelve las notificaciones enviadas en esta ejecución.
 */
public interface EnviarRecordatoriosUseCase {

    List<Notificacion> enviarRecordatorios();
}
