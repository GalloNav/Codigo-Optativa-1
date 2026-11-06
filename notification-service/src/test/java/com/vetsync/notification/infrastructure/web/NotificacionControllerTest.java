package com.vetsync.notification.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.notification.application.fakes.FakeListarNotificacionesUseCase;
import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.domain.LimiteDeListadoInvalidoException;
import com.vetsync.notification.domain.Notificacion;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP de {@code GET /notificaciones}. Solo la capa web: el caso de uso es un fake. */
@WebMvcTest(NotificacionController.class)
class NotificacionControllerTest {

    private static final Notificacion RECIENTE = new Notificacion(2L, 6L, "luis@correo.com",
            "Recordatorio de cita: Michi", "Hola Luis", LocalDateTime.of(2030, 6, 10, 11, 0));
    private static final Notificacion ANTIGUA = new Notificacion(1L, 5L, "ana@correo.com",
            "Recordatorio de cita: Rex", "Hola Ana", LocalDateTime.of(2030, 6, 10, 10, 0));

    /** Registra el fake como el bean del puerto de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeListarNotificacionesUseCase listarNotificacionesUseCase() {
            return new FakeListarNotificacionesUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeListarNotificacionesUseCase casoDeUso;

    @Autowired
    NotificacionControllerTest(MockMvc mockMvc, FakeListarNotificacionesUseCase casoDeUso) {
        this.mockMvc = mockMvc;
        this.casoDeUso = casoDeUso;
    }

    @BeforeEach
    void reiniciarElCasoDeUso() {
        casoDeUso.reiniciar();
    }

    @Test
    void devuelveLasNotificacionesEnElOrdenRecibido() throws Exception {
        casoDeUso.responderCon(List.of(RECIENTE, ANTIGUA));

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].citaId").value(6))
                .andExpect(jsonPath("$[0].destinatario").value("luis@correo.com"))
                .andExpect(jsonPath("$[0].asunto").value("Recordatorio de cita: Michi"))
                .andExpect(jsonPath("$[0].cuerpo").value("Hola Luis"))
                .andExpect(jsonPath("$[0].enviadaEn").value("2030-06-10T11:00:00"))
                .andExpect(jsonPath("$[1].citaId").value(5));
    }

    @Test
    void pideLos100RecordatoriosMasRecientesCuandoNoSeIndicaLimite() throws Exception {
        mockMvc.perform(get("/notificaciones")).andExpect(status().isOk());

        assertThat(casoDeUso.ultimoLimite()).isEqualTo(100);
    }

    @Test
    void pasaAlCasoDeUsoElLimiteIndicadoEnLaConsulta() throws Exception {
        mockMvc.perform(get("/notificaciones").param("limite", "5")).andExpect(status().isOk());

        assertThat(casoDeUso.ultimoLimite()).isEqualTo(5);
    }

    @Test
    void respondeBadRequestSinInvocarElCasoDeUsoCuandoElLimiteNoEsUnNumero() throws Exception {
        mockMvc.perform(get("/notificaciones").param("limite", "muchos"))
                .andExpect(status().isBadRequest());

        assertThat(casoDeUso.ultimoLimite()).isNull();
    }

    @Test
    void respondeBadRequestCuandoElLimiteNoEsValido() throws Exception {
        casoDeUso.fallarCon(new LimiteDeListadoInvalidoException("El limite debe estar entre 1 y 500"));

        mockMvc.perform(get("/notificaciones").param("limite", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El limite debe estar entre 1 y 500"));
    }

    @Test
    void devuelveUnArregloVacioCuandoNoSeHaEnviadoNada() throws Exception {
        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void respondeServiceUnavailableSinFiltrarElDetalleTecnico() throws Exception {
        casoDeUso.fallarCon(new AccesoADatosException("No se pudo consultar las notificaciones",
                new IllegalStateException("password=secreta")));

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(content().string(not(containsString("secreta"))));
    }

    @Test
    void respondeInternalServerErrorSinStacktraceAnteUnErrorInesperado() throws Exception {
        casoDeUso.fallarCon(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("detalle interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
