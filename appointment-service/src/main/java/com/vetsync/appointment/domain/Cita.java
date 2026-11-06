package com.vetsync.appointment.domain;

import java.time.LocalDateTime;

/**
 * Cita veterinaria. El {@code id} es nulo hasta que se persiste.
 * Los datos de la mascota y del contacto son una copia tomada al agendar.
 */
public record Cita(
        Long id,
        long veterinarioId,
        long mascotaId,
        String mascotaNombre,
        String contactoNombre,
        String contactoCorreo,
        LocalDateTime inicio,
        String motivo,
        EstadoCita estado) {
}
