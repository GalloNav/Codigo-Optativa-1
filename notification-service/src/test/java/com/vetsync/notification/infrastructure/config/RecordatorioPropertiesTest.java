package com.vetsync.notification.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * La anticipación no puede superar el rango máximo que appointment-service acepta en
 * {@code GET /citas/por-recordar} (7 días): si lo superara, cada revisión recibiría un 400 y no se enviaría nada.
 */
class RecordatorioPropertiesTest {

    private static final Duration INTERVALO = Duration.ofSeconds(60);
    private static final int SIETE_DIAS_EN_HORAS = 7 * 24;

    @Test
    void aceptaLaAnticipacionPorDefectoYElMaximoDeSieteDias() {
        assertThatCode(() -> new RecordatorioProperties(24, INTERVALO)).doesNotThrowAnyException();
        assertThatCode(() -> new RecordatorioProperties(SIETE_DIAS_EN_HORAS, INTERVALO)).doesNotThrowAnyException();
    }

    @Test
    void rechazaUnaAnticipacionMayorQueElRangoQueAceptaLaAgenda() {
        assertThatThrownBy(() -> new RecordatorioProperties(SIETE_DIAS_EN_HORAS + 1, INTERVALO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(SIETE_DIAS_EN_HORAS));
    }
}
