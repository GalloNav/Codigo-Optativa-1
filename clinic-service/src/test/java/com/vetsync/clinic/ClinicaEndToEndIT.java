package com.vetsync.clinic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Punta a punta de clinic-service: HTTP, casos de uso, adaptadores JPA y SQL Server real, con el cableado real. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClinicaEndToEndIT {

    private static final String PROPIETARIO = """
            {"nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234"}
            """;
    private static final String MASCOTA = """
            {"nombre": "Rex", "especie": "Perro", "raza": "Labrador", "sexo": "M", "fechaNacimiento": "2020-03-15"}
            """;

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private final MockMvc mockMvc;
    private final JdbcOperations jdbc;

    @Autowired
    ClinicaEndToEndIT(MockMvc mockMvc, JdbcOperations jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void limpiarTablas() {
        jdbc.update("DELETE FROM registro_medico");
        jdbc.update("DELETE FROM mascota");
        jdbc.update("DELETE FROM propietario");
    }

    @Test
    void registraAlPropietarioYSuMascotaYLaConsultaConElContratoDeAppointmentService() throws Exception {
        String propietarioCreado = mockMvc.perform(post("/propietarios")
                        .contentType(MediaType.APPLICATION_JSON).content(PROPIETARIO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer propietarioId = JsonPath.read(propietarioCreado, "$.id");

        String mascotaCreada = mockMvc.perform(post("/propietarios/{id}/mascotas", propietarioId)
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer mascotaId = JsonPath.read(mascotaCreada, "$.id");

        mockMvc.perform(get("/mascotas/{id}", mascotaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Rex"))
                .andExpect(jsonPath("$.sexo").value("M"))
                .andExpect(jsonPath("$.fechaNacimiento").value("2020-03-15"))
                .andExpect(jsonPath("$.propietario.id").value(propietarioId))
                .andExpect(jsonPath("$.propietario.nombre").value("Ana Perez"))
                .andExpect(jsonPath("$.propietario.correo").value("ana@correo.com"))
                .andExpect(jsonPath("$.propietario.telefono").doesNotExist());
    }

    @Test
    void rechazaUnCorreoDuplicadoConConflict() throws Exception {
        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON).content(PROPIETARIO))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON).content(PROPIETARIO))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM propietario", Integer.class)).isEqualTo(1);
    }

    @Test
    void respondeNotFoundParaUnaMascotaInexistente() throws Exception {
        mockMvc.perform(get("/mascotas/999999")).andExpect(status().isNotFound());
    }

    @Test
    void respondeNotFoundAlRegistrarUnaMascotaDeUnPropietarioInexistente() throws Exception {
        mockMvc.perform(post("/propietarios/999999/mascotas")
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA))
                .andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mascota", Integer.class)).isZero();
    }

    @Test
    void registraEntradasMedicasYLasListaDeLaMasRecienteALaMasAntigua() throws Exception {
        Integer mascotaId = registrarMascota();

        mockMvc.perform(post("/mascotas/{id}/historial", mascotaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "CONSULTA", "motivo": "Primera visita", "pesoKg": 12.5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
        mockMvc.perform(post("/mascotas/{id}/historial", mascotaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "VACUNA", "motivo": "Vacuna anual", "veterinarioId": 3}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/mascotas/{id}/historial", mascotaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].motivo").value("Vacuna anual"))
                .andExpect(jsonPath("$[0].veterinarioId").value(3))
                .andExpect(jsonPath("$[1].motivo").value("Primera visita"))
                .andExpect(jsonPath("$[1].pesoKg").value(12.5));
    }

    @Test
    void respondeNotFoundAlRegistrarOConsultarElHistorialDeUnaMascotaInexistente() throws Exception {
        mockMvc.perform(post("/mascotas/999999/historial")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "CONSULTA", "motivo": "Control"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/mascotas/999999/historial")).andExpect(status().isNotFound());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM registro_medico", Integer.class)).isZero();
    }

    private Integer registrarMascota() throws Exception {
        String propietario = mockMvc.perform(post("/propietarios")
                        .contentType(MediaType.APPLICATION_JSON).content(PROPIETARIO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer propietarioId = JsonPath.read(propietario, "$.id");
        String mascota = mockMvc.perform(post("/propietarios/{id}/mascotas", propietarioId)
                        .contentType(MediaType.APPLICATION_JSON).content(MASCOTA))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(mascota, "$.id");
    }
}
