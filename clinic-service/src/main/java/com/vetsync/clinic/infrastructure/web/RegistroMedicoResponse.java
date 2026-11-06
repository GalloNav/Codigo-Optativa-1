package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entrada del historial médico tal como la ve el cliente de la API. */
public record RegistroMedicoResponse(
        Long id,
        LocalDateTime fecha,
        TipoRegistro tipo,
        String motivo,
        String diagnostico,
        String tratamiento,
        BigDecimal pesoKg,
        Long veterinarioId) {

    public static RegistroMedicoResponse desde(RegistroMedico registro) {
        return new RegistroMedicoResponse(
                registro.id(),
                registro.fecha(),
                registro.tipo(),
                registro.motivo(),
                registro.diagnostico(),
                registro.tratamiento(),
                registro.pesoKg(),
                registro.veterinarioId());
    }
}
