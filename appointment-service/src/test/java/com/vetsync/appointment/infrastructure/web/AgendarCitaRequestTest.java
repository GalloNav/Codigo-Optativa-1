package com.vetsync.appointment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Validación del motivo de {@code POST /citas}: el largo de la columna (200) y sin caracteres de control. */
class AgendarCitaRequestTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 6, 11, 9, 0);
    private static final int LONGITUD_DE_LA_COLUMNA_MOTIVO = 200;

    @Test
    void aceptaUnMotivoDeExactamenteLaLongitudDeLaColumna() {
        assertThatCode(() -> cita("m".repeat(LONGITUD_DE_LA_COLUMNA_MOTIVO))).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnMotivoMasLargoQueLaColumna() {
        assertThatThrownBy(() -> cita("m".repeat(LONGITUD_DE_LA_COLUMNA_MOTIVO + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo")
                .hasMessageContaining(String.valueOf(LONGITUD_DE_LA_COLUMNA_MOTIVO));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Control\nINFO falso", "Control\rfalso", "Control\tanual", "Control\0", "\033[31mControl"})
    void rechazaUnMotivoConCaracteresDeControl(String motivo) {
        assertThatThrownBy(() -> cita(motivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo")
                .hasMessageContaining("control");
    }

    @Test
    void sigueRechazandoUnMotivoEnBlanco() {
        assertThatThrownBy(() -> cita("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motivo");
    }

    private static AgendarCitaRequest cita(String motivo) {
        return new AgendarCitaRequest(1L, 7L, INICIO, motivo);
    }
}
