package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaComando;
import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cuerpo de {@code POST /mascotas/{id}/historial}. Tipo y motivo (máximo 200 caracteres) son obligatorios;
 * diagnóstico y tratamiento son opcionales y admiten hasta 4000 caracteres (la columna no tiene tope, este lo fija
 * el servicio); el peso, si viene, debe estar entre 0 (exclusivo) y 999.99 kg, y el veterinario debe ser un id
 * positivo. Los demás límites reflejan las columnas de la base de datos. Si el cuerpo no cumple, Spring responde 400.
 */
public record RegistrarEntradaMedicaRequest(
        TipoRegistro tipo,
        String motivo,
        String diagnostico,
        String tratamiento,
        BigDecimal pesoKg,
        Long veterinarioId) {

    private static final int LONGITUD_MAXIMA_DEL_MOTIVO = 200;
    private static final int LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO = 4_000;
    private static final BigDecimal PESO_MAXIMO_EN_KG = new BigDecimal("999.99");

    public RegistrarEntradaMedicaRequest {
        Objects.requireNonNull(tipo, "tipo es obligatorio");
        ValidacionDeTexto.requerirLinea(motivo, "motivo", LONGITUD_MAXIMA_DEL_MOTIVO);
        ValidacionDeTexto.admitirParrafo(diagnostico, "diagnostico", LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO);
        ValidacionDeTexto.admitirParrafo(tratamiento, "tratamiento", LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO);
        if (pesoKg != null && (pesoKg.signum() <= 0 || pesoKg.compareTo(PESO_MAXIMO_EN_KG) > 0)) {
            throw new IllegalArgumentException("pesoKg debe ser mayor que 0 y no superar " + PESO_MAXIMO_EN_KG);
        }
        if (veterinarioId != null && veterinarioId <= 0) {
            throw new IllegalArgumentException("veterinarioId debe ser un id positivo");
        }
    }

    public RegistrarEntradaMedicaComando aComando(long mascotaId) {
        return new RegistrarEntradaMedicaComando(
                mascotaId, tipo, motivo, diagnostico, tratamiento, pesoKg, veterinarioId);
    }
}
