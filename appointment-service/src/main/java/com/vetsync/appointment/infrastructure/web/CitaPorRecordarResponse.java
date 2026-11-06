package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.domain.Cita;
import java.time.LocalDateTime;

/**
 * Cita por recordar. Esta forma es el contrato que consume notification-service
 * ({@code GET /citas/por-recordar}); incluye el contacto del dueño porque es un endpoint interno entre servicios.
 */
public record CitaPorRecordarResponse(
        Long id,
        LocalDateTime inicio,
        String mascotaNombre,
        String contactoNombre,
        String contactoCorreo,
        String motivo) {

    public static CitaPorRecordarResponse desde(Cita cita) {
        return new CitaPorRecordarResponse(cita.id(), cita.inicio(), cita.mascotaNombre(), cita.contactoNombre(),
                cita.contactoCorreo(), cita.motivo());
    }
}
