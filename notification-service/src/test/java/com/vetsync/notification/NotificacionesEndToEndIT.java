package com.vetsync.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.notification.application.fakes.FakeAgendaGateway;
import com.vetsync.notification.application.port.in.EnviarRecordatoriosUseCase;
import com.vetsync.notification.domain.CitaPorRecordar;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Punta a punta de notification-service: caso de uso, correo simulado, SQL Server real y {@code GET /notificaciones},
 * con el cableado de producción. Solo se simula lo externo al servicio: appointment-service (fake) y el reloj (fijo).
 */
@SpringBootTest(properties = "vetsync.recordatorio.intervalo=1h")
@AutoConfigureMockMvc
@Testcontainers
class NotificacionesEndToEndIT {

    private static final CitaPorRecordar CITA_DE_REX = new CitaPorRecordar(
            5L, LocalDateTime.of(2030, 6, 10, 18, 0), "Rex", "Ana Perez", "ana@correo.com", "Control anual");
    private static final CitaPorRecordar CITA_DE_MICHI = new CitaPorRecordar(
            6L, LocalDateTime.of(2030, 6, 11, 9, 30), "Michi", "Luis Mora", "luis@correo.com", "Vacuna anual");

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    /** Sustituye únicamente lo que vive fuera de este servicio. */
    @TestConfiguration
    static class ExternosSimulados {

        @Bean
        @Primary
        FakeAgendaGateway agendaGatewayFalso() {
            return new FakeAgendaGateway();
        }

        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(LocalDateTime.of(2030, 6, 10, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    private final MockMvc mockMvc;
    private final JdbcOperations jdbc;
    private final FakeAgendaGateway agendaGateway;
    private final EnviarRecordatoriosUseCase enviarRecordatorios;

    @Autowired
    NotificacionesEndToEndIT(
            MockMvc mockMvc,
            JdbcOperations jdbc,
            FakeAgendaGateway agendaGateway,
            EnviarRecordatoriosUseCase enviarRecordatorios) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
        this.agendaGateway = agendaGateway;
        this.enviarRecordatorios = enviarRecordatorios;
    }

    @BeforeEach
    void prepararEscenario() {
        jdbc.update("DELETE FROM notificacion");
        agendaGateway.reiniciar();
    }

    @Test
    void enviaLosRecordatoriosLosListaYNoLosDuplicaEnLaSiguienteEjecucion() throws Exception {
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI));

        assertThat(enviarRecordatorios.enviarRecordatorios()).hasSize(2);

        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].citaId").value(6))
                .andExpect(jsonPath("$[0].destinatario").value("luis@correo.com"))
                .andExpect(jsonPath("$[0].asunto").value("Recordatorio de cita: Michi"))
                .andExpect(jsonPath("$[0].cuerpo", containsString("11/06/2030 09:30")))
                .andExpect(jsonPath("$[0].enviadaEn").value("2030-06-10T10:00:00"))
                .andExpect(jsonPath("$[1].citaId").value(5));

        assertThat(enviarRecordatorios.enviarRecordatorios()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notificacion", Integer.class)).isEqualTo(2);
    }

    @Test
    void elLimiteDeLaConsultaAcotaElListadoALasMasRecientes() throws Exception {
        agendaGateway.responderCon(List.of(CITA_DE_REX, CITA_DE_MICHI));
        enviarRecordatorios.enviarRecordatorios();

        mockMvc.perform(get("/notificaciones").param("limite", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].citaId").value(6));
    }

    @Test
    void respondeBadRequestSiElLimiteEstaFueraDeRango() throws Exception {
        mockMvc.perform(get("/notificaciones").param("limite", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/notificaciones").param("limite", "501")).andExpect(status().isBadRequest());
    }

    @Test
    void listaUnArregloVacioCuandoAunNoSeHaEnviadoNada() throws Exception {
        mockMvc.perform(get("/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
