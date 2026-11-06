package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.clinic.domain.TipoRegistro;
import org.junit.jupiter.api.Test;

/**
 * Largos de {@code POST /mascotas/{id}/historial}. Diagnóstico y tratamiento son NVARCHAR(MAX) en la base, así que
 * el servicio fija su propio tope para que una sola petición no pueda llenar la memoria ni la tabla.
 */
class RegistrarEntradaMedicaRequestTest {

    private static final int LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO = 4_000;

    @Test
    void aceptaDiagnosticoYTratamientoDeExactamenteElTopeYConSaltosDeLinea() {
        String texto = "l".repeat(LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO - 2) + "\r\n";

        assertThatCode(() -> entrada("Control", texto, texto)).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnDiagnosticoMasLargoQueElTope() {
        String diagnostico = "d".repeat(LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO + 1);

        assertThatThrownBy(() -> entrada("Control", diagnostico, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostico");
    }

    @Test
    void rechazaUnTratamientoMasLargoQueElTope() {
        String tratamiento = "t".repeat(LONGITUD_MAXIMA_DE_DIAGNOSTICO_Y_TRATAMIENTO + 1);

        assertThatThrownBy(() -> entrada("Control", null, tratamiento))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tratamiento");
    }

    @Test
    void rechazaCaracteresDeControlQueNoSeanSaltosDeLineaOTabuladores() {
        assertThatThrownBy(() -> entrada("Control", "Dosis\0", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostico");
        assertThatThrownBy(() -> entrada("Control", null, "\033[31mReposo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tratamiento");
    }

    @Test
    void rechazaUnMotivoConSaltosDeLinea() {
        assertThatThrownBy(() -> entrada("Control\nINFO falso", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void sigueRechazandoUnMotivoDeMasDe200Caracteres() {
        assertThatThrownBy(() -> entrada("m".repeat(201), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }

    private static RegistrarEntradaMedicaRequest entrada(String motivo, String diagnostico, String tratamiento) {
        return new RegistrarEntradaMedicaRequest(TipoRegistro.CONSULTA, motivo, diagnostico, tratamiento, null, null);
    }
}
