package com.vetsync.notification.domain;

import java.time.LocalDateTime;

/** Datos de una cita próxima que appointment-service entrega para enviar el recordatorio. */
public record CitaPorRecordar(
        long id,
        LocalDateTime inicio,
        String mascotaNombre,
        String contactoNombre,
        String contactoCorreo,
        String motivo) {
}
