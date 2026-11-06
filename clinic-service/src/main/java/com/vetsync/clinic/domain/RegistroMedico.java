package com.vetsync.clinic.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entrada del historial médico de una mascota. El {@code id} es nulo hasta que se persiste.
 * La fecha la pone el sistema con su reloj. El veterinario es una referencia por id a appointment-service:
 * clinic-service no puede validarla porque no puede consultar a ese servicio.
 */
public record RegistroMedico(
        Long id,
        long mascotaId,
        LocalDateTime fecha,
        TipoRegistro tipo,
        String motivo,
        String diagnostico,
        String tratamiento,
        BigDecimal pesoKg,
        Long veterinarioId) {
}
