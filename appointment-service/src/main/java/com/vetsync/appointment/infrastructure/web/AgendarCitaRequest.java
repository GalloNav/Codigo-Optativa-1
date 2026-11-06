package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cuerpo de {@code POST /citas}. El inicio es hora local de la clínica en formato ISO-8601.
 * Los campos son obligatorios: si falta alguno, la deserialización falla y Spring responde 400. El motivo admite
 * hasta 200 caracteres (la columna) y no puede llevar caracteres de control.
 */
public record AgendarCitaRequest(Long veterinarioId, Long mascotaId, LocalDateTime inicio, String motivo) {

    /** Largo de la columna {@code cita.motivo}. */
    private static final int LONGITUD_MAXIMA_DEL_MOTIVO = 200;

    public AgendarCitaRequest {
        Objects.requireNonNull(veterinarioId, "veterinarioId es obligatorio");
        Objects.requireNonNull(mascotaId, "mascotaId es obligatorio");
        Objects.requireNonNull(inicio, "inicio es obligatorio");
        Objects.requireNonNull(motivo, "motivo es obligatorio");
        if (motivo.isBlank()) {
            throw new IllegalArgumentException("motivo no puede estar vacio");
        }
        if (motivo.length() > LONGITUD_MAXIMA_DEL_MOTIVO) {
            throw new IllegalArgumentException("motivo no puede superar " + LONGITUD_MAXIMA_DEL_MOTIVO + " caracteres");
        }
        if (motivo.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("motivo no puede contener caracteres de control");
        }
    }

    public AgendarCitaComando aComando() {
        return new AgendarCitaComando(veterinarioId, mascotaId, inicio, motivo);
    }
}
