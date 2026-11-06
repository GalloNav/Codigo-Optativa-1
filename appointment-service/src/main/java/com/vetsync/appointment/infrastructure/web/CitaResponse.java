package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.EstadoCita;
import java.time.LocalDateTime;

/** Cita agendada tal como la ve el cliente de la API (sin datos de contacto del dueño). */
public record CitaResponse(
        Long id,
        long veterinarioId,
        long mascotaId,
        String mascotaNombre,
        LocalDateTime inicio,
        String motivo,
        EstadoCita estado) {

    public static CitaResponse desde(Cita cita) {
        return new CitaResponse(cita.id(), cita.veterinarioId(), cita.mascotaId(), cita.mascotaNombre(),
                cita.inicio(), cita.motivo(), cita.estado());
    }
}
