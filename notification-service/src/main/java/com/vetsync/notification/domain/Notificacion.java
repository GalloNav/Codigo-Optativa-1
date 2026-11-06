package com.vetsync.notification.domain;

import java.time.LocalDateTime;

/**
 * Recordatorio de cita ya enviado (de forma simulada). El {@code id} es nulo hasta que se persiste.
 * Hay a lo sumo una notificación por cita.
 */
public record Notificacion(
        Long id,
        long citaId,
        String destinatario,
        String asunto,
        String cuerpo,
        LocalDateTime enviadaEn) {
}
