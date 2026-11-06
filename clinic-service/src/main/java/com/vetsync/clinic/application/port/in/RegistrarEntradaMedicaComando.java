package com.vetsync.clinic.application.port.in;

import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;

/** Datos de una entrada médica; diagnóstico, tratamiento, peso y veterinario son opcionales. */
public record RegistrarEntradaMedicaComando(
        long mascotaId,
        TipoRegistro tipo,
        String motivo,
        String diagnostico,
        String tratamiento,
        BigDecimal pesoKg,
        Long veterinarioId) {
}
