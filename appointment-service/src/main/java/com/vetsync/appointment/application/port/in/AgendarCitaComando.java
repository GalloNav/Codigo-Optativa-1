package com.vetsync.appointment.application.port.in;

import java.time.LocalDateTime;

/** Datos necesarios para agendar una cita; el inicio es hora local de la clínica. */
public record AgendarCitaComando(long veterinarioId, long mascotaId, LocalDateTime inicio, String motivo) {
}
