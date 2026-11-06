package com.vetsync.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.vetsync.appointment.application.fakes.FakeClinicaGateway;
import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Punta a punta de {@code POST /citas}: HTTP, caso de uso, adaptador JPA y SQL Server real, con el cableado
 * de producción. Solo se simula lo externo al servicio: clinic-service (fake) y el reloj (fijo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CitasEndToEndIT {

    private static final long MASCOTA_ID = 7L;
    private static final long MASCOTA_NO_REGISTRADA_ID = 999L;

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    /** Sustituye únicamente lo que vive fuera de este servicio. */
    @TestConfiguration
    static class ExternosSimulados {

        @Bean
        @Primary
        ClinicaGateway clinicaGatewayFalso() {
            FakeClinicaGateway clinicaGateway = new FakeClinicaGateway();
            clinicaGateway.registrar(MASCOTA_ID, new ContactoDeMascota("Rex", "Ana Perez", "ana@correo.com"));
            return clinicaGateway;
        }

        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(LocalDateTime.of(2030, 6, 10, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    private final MockMvc mockMvc;
    private final JdbcOperations jdbc;
    private long veterinarioId;

    @Autowired
    CitasEndToEndIT(MockMvc mockMvc, JdbcOperations jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void prepararVeterinario() {
        jdbc.update("DELETE FROM cita");
        jdbc.update("DELETE FROM veterinario");
        veterinarioId = jdbc.queryForObject(
                "INSERT INTO veterinario (nombre) OUTPUT INSERTED.id VALUES ('Dra. Prueba')", Long.class);
    }

    @Test
    void agendaLaCitaYRechazaUnSegundoIntentoEnLaMismaFranja() throws Exception {
        String cuerpo = cuerpoDe(MASCOTA_ID, "2030-06-11T09:00:00");

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"))
                .andExpect(jsonPath("$.mascotaNombre").value("Rex"));
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class)).isEqualTo(1);
    }

    @Test
    void respondeNotFoundCuandoLaMascotaNoEstaRegistrada() throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDe(MASCOTA_NO_REGISTRADA_ID, "2030-06-11T09:00:00")))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class)).isZero();
    }

    @Test
    void respondeUnprocessableEntityCuandoLaFranjaYaPaso() throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDe(MASCOTA_ID, "2030-06-09T09:00:00")))
                .andExpect(status().isUnprocessableEntity());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class)).isZero();
    }

    @ParameterizedTest(name = "inicio {0}")
    @ValueSource(strings = {"2030-06-11T09:15:00", "2030-06-11T07:30:00", "2030-06-11T18:00:00"})
    void respondeUnprocessableEntityCuandoElInicioNoCaeEnUnaFranjaDeLaAgenda(String inicio) throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDe(MASCOTA_ID, inicio)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class)).isZero();
    }

    @Test
    void registraUnVeterinarioPorLaApiYAgendaConEl() throws Exception {
        String creado = mockMvc.perform(post("/veterinarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana Gomez", "especialidad": "Cirugia"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Ana Gomez"))
                .andReturn().getResponse().getContentAsString();
        Integer nuevoVeterinarioId = JsonPath.read(creado, "$.id");

        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veterinarioId": %d, "mascotaId": %d, "inicio": "2030-06-11T10:00:00",
                                 "motivo": "Control anual"}
                                """.formatted(nuevoVeterinarioId, MASCOTA_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.veterinarioId").value(nuevoVeterinarioId));

        mockMvc.perform(get("/veterinarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Ana Gomez"))
                .andExpect(jsonPath("$[0].especialidad").value("Cirugia"))
                .andExpect(jsonPath("$[1].nombre").value("Dra. Prueba"));
    }

    @Test
    void respondeNotFoundCuandoElVeterinarioNoEstaRegistrado() throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veterinarioId": 999999, "mascotaId": %d, "inicio": "2030-06-11T09:00:00",
                                 "motivo": "Control anual"}
                                """.formatted(MASCOTA_ID)))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cita", Integer.class)).isZero();
    }

    @Test
    void listaLasCitasPorRecordarConElContactoDelDueno() throws Exception {
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDe(MASCOTA_ID, "2030-06-11T09:00:00")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/citas").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoDe(MASCOTA_ID, "2030-06-12T09:00:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/citas/por-recordar")
                        .param("desde", "2030-06-11T00:00:00")
                        .param("hasta", "2030-06-11T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].inicio").value("2030-06-11T09:00:00"))
                .andExpect(jsonPath("$[0].mascotaNombre").value("Rex"))
                .andExpect(jsonPath("$[0].contactoNombre").value("Ana Perez"))
                .andExpect(jsonPath("$[0].contactoCorreo").value("ana@correo.com"));
    }

    private String cuerpoDe(long mascotaId, String inicio) {
        return """
                {"veterinarioId": %d, "mascotaId": %d, "inicio": "%s", "motivo": "Control anual"}
                """.formatted(veterinarioId, mascotaId, inicio);
    }
}
