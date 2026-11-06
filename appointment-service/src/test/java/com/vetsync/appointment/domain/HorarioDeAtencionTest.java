package com.vetsync.appointment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Especificación del horario de atención: franjas de 30 minutos alineadas a la media hora, dentro del horario. */
class HorarioDeAtencionTest {

    private static final LocalDate DIA = LocalDate.of(2030, 6, 11);

    private final HorarioDeAtencion horario = new HorarioDeAtencion(8, 18);

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"08:00", "08:30", "12:00", "17:30"})
    void admiteElInicioDeCualquierFranjaDentroDelHorario(String hora) {
        assertThat(horario.admite(DIA.atTime(LocalTime.parse(hora)))).isTrue();
    }

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"09:01", "09:15", "09:29", "09:45"})
    void rechazaUnInicioQueNoCaeEnLaMediaHora(String hora) {
        assertThat(horario.admite(DIA.atTime(LocalTime.parse(hora)))).isFalse();
    }

    @Test
    void rechazaUnInicioConSegundos() {
        assertThat(horario.admite(LocalDateTime.of(DIA, LocalTime.of(9, 0, 30)))).isFalse();
    }

    @Test
    void rechazaUnInicioConFraccionDeSegundo() {
        assertThat(horario.admite(LocalDateTime.of(DIA, LocalTime.of(9, 0, 0, 1)))).isFalse();
    }

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"00:00", "07:30"})
    void rechazaUnInicioAnteriorALaApertura(String hora) {
        assertThat(horario.admite(DIA.atTime(LocalTime.parse(hora)))).isFalse();
    }

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"18:00", "18:30", "23:30"})
    void rechazaUnInicioCuyaFranjaTerminaDespuesDelCierre(String hora) {
        assertThat(horario.admite(DIA.atTime(LocalTime.parse(hora)))).isFalse();
    }

    @Test
    void admiteLaUltimaFranjaDelDiaCuandoElCierreEsALaMedianoche() {
        HorarioDeAtencion horarioDeTodoElDia = new HorarioDeAtencion(0, 24);

        assertThat(horarioDeTodoElDia.admite(DIA.atTime(0, 0))).isTrue();
        assertThat(horarioDeTodoElDia.admite(DIA.atTime(23, 30))).isTrue();
    }

    @ParameterizedTest(name = "apertura {0}, cierre {1}")
    @CsvSource({"-1, 18", "8, 25", "18, 18", "18, 8"})
    void rechazaUnHorarioConHorasInvalidas(int horaApertura, int horaCierre) {
        assertThatThrownBy(() -> new HorarioDeAtencion(horaApertura, horaCierre))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
