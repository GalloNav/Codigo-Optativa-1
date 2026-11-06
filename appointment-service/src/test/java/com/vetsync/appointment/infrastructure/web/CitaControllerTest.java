package com.vetsync.appointment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.appointment.application.fakes.FakeAgendarCitaUseCase;
import com.vetsync.appointment.application.port.in.AgendarCitaComando;
import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.domain.Cita;
import com.vetsync.appointment.domain.ConflictoDeHorarioException;
import com.vetsync.appointment.domain.EstadoCita;
import com.vetsync.appointment.domain.FranjaHorariaInvalidaException;
import com.vetsync.appointment.domain.MascotaNoRegistradaException;
import com.vetsync.appointment.domain.VeterinarioNoEncontradoException;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP de {@code POST /citas}. Solo la capa web: el caso de uso es un fake y no hay base de datos.
 */
@WebMvcTest(CitaController.class)
class CitaControllerTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2030, 6, 11, 9, 0);
    private static final String CUERPO_VALIDO = """
            {"veterinarioId": 1, "mascotaId": 7, "inicio": "2030-06-11T09:00:00", "motivo": "Control anual"}
            """;

    /** Registra el fake como el bean del puerto de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeAgendarCitaUseCase agendarCitaUseCase() {
            return new FakeAgendarCitaUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeAgendarCitaUseCase casoDeUso;

    @Autowired
    CitaControllerTest(MockMvc mockMvc, FakeAgendarCitaUseCase casoDeUso) {
        this.mockMvc = mockMvc;
        this.casoDeUso = casoDeUso;
    }

    @BeforeEach
    void reiniciarElCasoDeUso() {
        casoDeUso.reiniciar();
    }

    @Test
    void respondeCreatedConLaCitaAgendadaSinExponerElContacto() throws Exception {
        casoDeUso.responderCon(new Cita(100L, 1L, 7L, "Rex", "Ana Perez", "ana@correo.com",
                INICIO, "Control anual", EstadoCita.PROGRAMADA));

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.veterinarioId").value(1))
                .andExpect(jsonPath("$.mascotaId").value(7))
                .andExpect(jsonPath("$.mascotaNombre").value("Rex"))
                .andExpect(jsonPath("$.inicio").value("2030-06-11T09:00:00"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.contactoCorreo").doesNotExist());

        assertThat(casoDeUso.ultimoComando()).isEqualTo(new AgendarCitaComando(1L, 7L, INICIO, "Control anual"));
    }

    @ParameterizedTest
    @MethodSource("erroresDeNegocio")
    void traduceLosErroresDeNegocioAlCodigoHttpCorrespondiente(RuntimeException error, HttpStatus estado)
            throws Exception {
        casoDeUso.fallarCon(error);

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().is(estado.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(estado.value()))
                .andExpect(jsonPath("$.detail").value(error.getMessage()));
    }

    static Stream<Arguments> erroresDeNegocio() {
        return Stream.of(
                arguments(new ConflictoDeHorarioException("Franja ocupada"), HttpStatus.CONFLICT),
                arguments(new FranjaHorariaInvalidaException("Franja pasada"), HttpStatus.UNPROCESSABLE_ENTITY),
                arguments(new MascotaNoRegistradaException("Mascota 7 no registrada"), HttpStatus.NOT_FOUND),
                arguments(new VeterinarioNoEncontradoException("Veterinario 1 no registrado"), HttpStatus.NOT_FOUND));
    }

    @Test
    void respondeServiceUnavailableSinFiltrarElDetalleTecnico() throws Exception {
        casoDeUso.fallarCon(new AccesoADatosException("No se pudo guardar la cita",
                new IllegalStateException("password=secreta")));

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(content().string(not(containsString("secreta"))));
    }

    @Test
    void respondeInternalServerErrorSinStacktraceAnteUnErrorInesperado() throws Exception {
        casoDeUso.fallarCon(new IllegalStateException("detalle interno"));

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("detalle interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void respondeBadRequestSinInvocarElCasoDeUsoCuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        assertThat(casoDeUso.ultimoComando()).isNull();
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                """
                {"veterinarioId": 1}
                """,
                """
                {"veterinarioId": 1, "mascotaId": 7, "inicio": "ayer", "motivo": "Control anual"}
                """,
                """
                {"veterinarioId": 1, "mascotaId": 7, "inicio": "2030-06-11T09:00:00", "motivo": "   "}
                """,
                "esto no es json");
    }
}
