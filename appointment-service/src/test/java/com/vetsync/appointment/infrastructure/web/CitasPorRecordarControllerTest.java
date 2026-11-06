package com.vetsync.appointment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.appointment.application.fakes.FakeListarCitasPorRecordarUseCase;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.RangoDeFechasInvalidoException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP de {@code GET /citas/por-recordar}, el que consume notification-service.
 * Solo la capa web: el caso de uso es un fake.
 */
@WebMvcTest(CitasPorRecordarController.class)
class CitasPorRecordarControllerTest {

    private static final String RUTA_VALIDA =
            "/citas/por-recordar?desde=2030-06-10T10:00:00&hasta=2030-06-11T10:00:00";
    private static final Cita CITA = new Cita(5L, 1L, 7L, "Rex", "Ana Perez", "ana@correo.com",
            LocalDateTime.of(2030, 6, 10, 18, 0), "Control anual", EstadoCita.PROGRAMADA);

    /** Registra el fake como el bean del puerto de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeListarCitasPorRecordarUseCase listarCitasPorRecordarUseCase() {
            return new FakeListarCitasPorRecordarUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeListarCitasPorRecordarUseCase casoDeUso;

    @Autowired
    CitasPorRecordarControllerTest(MockMvc mockMvc, FakeListarCitasPorRecordarUseCase casoDeUso) {
        this.mockMvc = mockMvc;
        this.casoDeUso = casoDeUso;
    }

    @BeforeEach
    void reiniciarElCasoDeUso() {
        casoDeUso.reiniciar();
    }

    @Test
    void devuelveLasCitasPorRecordarConElContratoQueConsumeNotificationService() throws Exception {
        casoDeUso.responderCon(List.of(CITA));

        mockMvc.perform(get(RUTA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-10T18:00:00"))
                .andExpect(jsonPath("$[0].mascotaNombre").value("Rex"))
                .andExpect(jsonPath("$[0].contactoNombre").value("Ana Perez"))
                .andExpect(jsonPath("$[0].contactoCorreo").value("ana@correo.com"))
                .andExpect(jsonPath("$[0].motivo").value("Control anual"));

        assertThat(casoDeUso.ultimoDesde()).isEqualTo(LocalDateTime.of(2030, 6, 10, 10, 0));
        assertThat(casoDeUso.ultimoHasta()).isEqualTo(LocalDateTime.of(2030, 6, 11, 10, 0));
    }

    @Test
    void devuelveUnArregloVacioCuandoNoHayCitasPorRecordar() throws Exception {
        mockMvc.perform(get(RUTA_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void respondeBadRequestCuandoElRangoEsInvalido() throws Exception {
        casoDeUso.fallarCon(new RangoDeFechasInvalidoException("desde no puede ser posterior a hasta"));

        mockMvc.perform(get(RUTA_VALIDA))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("desde no puede ser posterior a hasta"));
    }

    @ParameterizedTest
    @MethodSource("rutasInvalidas")
    void respondeBadRequestSinInvocarElCasoDeUsoCuandoFaltanOSonIlegiblesLosParametros(String ruta)
            throws Exception {
        mockMvc.perform(get(ruta)).andExpect(status().isBadRequest());

        assertThat(casoDeUso.ultimoDesde()).isNull();
    }

    static Stream<String> rutasInvalidas() {
        return Stream.of(
                "/citas/por-recordar",
                "/citas/por-recordar?desde=2030-06-10T10:00:00",
                "/citas/por-recordar?hasta=2030-06-11T10:00:00",
                "/citas/por-recordar?desde=ayer&hasta=manana");
    }
}
