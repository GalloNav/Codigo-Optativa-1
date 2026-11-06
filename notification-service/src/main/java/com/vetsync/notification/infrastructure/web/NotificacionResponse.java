package com.vetsync.notification.infrastructure.web;

import com.vetsync.notification.domain.Notificacion;
import java.time.LocalDateTime;

/** Recordatorio enviado, tal como lo ve el cliente de la API. */
public record NotificacionResponse(
        Long id,
        long citaId,
        String destinatario,
        String asunto,
        String cuerpo,
        LocalDateTime enviadaEn) {

    public static NotificacionResponse desde(Notificacion notificacion) {
        return new NotificacionResponse(notificacion.id(), notificacion.citaId(), notificacion.destinatario(),
                notificacion.asunto(), notificacion.cuerpo(), notificacion.enviadaEn());
    }
}
