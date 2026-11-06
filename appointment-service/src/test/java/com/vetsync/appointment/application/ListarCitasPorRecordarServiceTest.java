package com.vetsync.appointment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsync.appointment.application.fakes.InMemoryCitaRepository;
import com.vetsync.appointment.application.port.in.ListarCitasPorRecordarUseCase;
import com.vetsync.appointment.application.service.ListarCitasPorRecordarService;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.RangoDeFechasInvalidoException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Especificación de {@code listarCitasPorRecordar}, sin Spring ni base de datos. */
class ListarCitasPorRecordarServiceTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2030, 6, 11, 10, 0);

    private InMemoryCitaRepository citaRepository;
    private ListarCitasPorRecordarUseCase casoDeUso;

    @BeforeEach
    void prepararEscenario() {
        citaRepository = new InMemoryCitaRepository();
        casoDeUso = new ListarCitasPorRecordarService(citaRepository);
    }

    @Test
    void devuelveLasCitasProgramadasDelRangoEnOrdenCronologico() {
        Cita tarde = guardar(HASTA.minusHours(2), EstadoCita.PROGRAMADA);
        Cita temprano = guardar(DESDE.plusHours(1), EstadoCita.PROGRAMADA);
        guardar(HASTA.plusMinutes(30), EstadoCita.PROGRAMADA);
        guardar(DESDE.minusMinutes(30), EstadoCita.PROGRAMADA);

        assertThat(casoDeUso.listarCitasPorRecordar(DESDE, HASTA)).containsExactly(temprano, tarde);
    }

    @Test
    void incluyeLasCitasQueEmpiezanExactamenteEnLosExtremosDelRango() {
        Cita alInicio = guardar(DESDE, EstadoCita.PROGRAMADA);
        Cita alFinal = guardar(HASTA, EstadoCita.PROGRAMADA);

        assertThat(casoDeUso.listarCitasPorRecordar(DESDE, HASTA)).containsExactly(alInicio, alFinal);
    }

    @Test
    void ignoraLasCitasCanceladas() {
        guardar(DESDE.plusHours(1), EstadoCita.CANCELADA);

        assertThat(casoDeUso.listarCitasPorRecordar(DESDE, HASTA)).isEmpty();
    }

    @Test
    void aceptaUnRangoDeUnSoloInstante() {
        Cita exacta = guardar(DESDE, EstadoCita.PROGRAMADA);

        assertThat(casoDeUso.listarCitasPorRecordar(DESDE, DESDE)).containsExactly(exacta);
    }

    @Test
    void aceptaUnRangoDeExactamenteElMaximoDeSieteDias() {
        Cita alFinal = guardar(DESDE.plusDays(7), EstadoCita.PROGRAMADA);

        assertThat(casoDeUso.listarCitasPorRecordar(DESDE, DESDE.plusDays(7))).containsExactly(alFinal);
    }

    @Test
    void rechazaUnRangoMasLargoQueElMaximoParaNoVolcarTodaLaAgenda() {
        LocalDateTime inicioDeLosTiempos = LocalDateTime.of(1, 1, 1, 0, 0);
        LocalDateTime finDeLosTiempos = LocalDateTime.of(9999, 12, 31, 23, 59);

        assertThatThrownBy(() -> casoDeUso.listarCitasPorRecordar(DESDE, DESDE.plusDays(7).plusSeconds(1)))
                .isInstanceOf(RangoDeFechasInvalidoException.class);
        assertThatThrownBy(() -> casoDeUso.listarCitasPorRecordar(inicioDeLosTiempos, finDeLosTiempos))
                .isInstanceOf(RangoDeFechasInvalidoException.class);
    }

    @Test
    void rechazaUnRangoCuyoInicioEsPosteriorAlFin() {
        assertThatThrownBy(() -> casoDeUso.listarCitasPorRecordar(HASTA, DESDE))
                .isInstanceOf(RangoDeFechasInvalidoException.class);
    }

    private Cita guardar(LocalDateTime inicio, EstadoCita estado) {
        return citaRepository.guardar(new Cita(
                null, 1L, 7L, "Rex", "Ana Perez", "ana@correo.com", inicio, "Control anual", estado));
    }
}
