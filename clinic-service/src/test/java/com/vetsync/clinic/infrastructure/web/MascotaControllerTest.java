package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.clinic.application.fakes.FakeConsultarMascotaUseCase;
import com.vetsync.clinic.application.fakes.FakeRegistrarMascotaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarMascotaComando;
import com.vetsync.clinic.application.port.out.AccesoADatosException;
import com.vetsync.clinic.domain.Mascota;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.Propietario;
import com.vetsync.clinic.domain.PropietarioNoRegistradoException;
import com.vetsync.clinic.domain.Sexo;
import java.time.LocalDate;
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
 * Contrato HTTP de las mascotas. {@code GET /mascotas/{id}} es el contrato que consume appointment-service.
 * Solo la capa web: los casos de uso son fakes.
 */
@WebMvcTest(MascotaController.class)
class MascotaControllerTest {

    private static final LocalDate NACIMIENTO_DE_REX = LocalDate.of(2020, 3, 15);
    private static final Propietario ANA = new Propietario(1L, "Ana Perez", "ana@correo.com", "555-1234");
    private static final MascotaConPropietario REX = new MascotaConPropietario(
            new Mascota(7L, 1L, "Rex", "Perro", "Labrador", Sexo.M, NACIMIENTO_DE_REX), ANA);
    private static final String CUERPO_VALIDO = """
            {"nombre": "Rex", "especie": "Perro", "raza": "Labrador", "sexo": "M", "fechaNacimiento": "2020-03-15"}
            """;

    /** Registra los fakes como los beans de los puertos de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeRegistrarMascotaUseCase registrarMascotaUseCase() {
            return new FakeRegistrarMascotaUseCase();
        }

        @Bean
        FakeConsultarMascotaUseCase consultarMascotaUseCase() {
            return new FakeConsultarMascotaUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeRegistrarMascotaUseCase registrarMascota;
    private final FakeConsultarMascotaUseCase consultarMascota;

    @Autowired
    MascotaControllerTest(
            MockMvc mockMvc, FakeRegistrarMascotaUseCase registrarMascota, FakeConsultarMascotaUseCase consultarMascota) {
        this.mockMvc = mockMvc;
        this.registrarMascota = registrarMascota;
        this.consultarMascota = consultarMascota;
    }

    @BeforeEach
    void reiniciarLosCasosDeUso() {
        registrarMascota.reiniciar();
        consultarMascota.reiniciar();
    }

    @Test
    void consultaDevuelveLaMascotaConElContratoQueConsumeAppointmentService() throws Exception {
        consultarMascota.responderCon(REX);

        mockMvc.perform(get("/mascotas/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Rex"))
                .andExpect(jsonPath("$.especie").value("Perro"))
                .andExpect(jsonPath("$.raza").value("Labrador"))
                .andExpect(jsonPath("$.sexo").value("M"))
                .andExpect(jsonPath("$.fechaNacimiento").value("2020-03-15"))
                .andExpect(jsonPath("$.propietario.id").value(1))
                .andExpect(jsonPath("$.propietario.nombre").value("Ana Perez"))
                .andExpect(jsonPath("$.propietario.correo").value("ana@correo.com"))
                .andExpect(jsonPath("$.propietario.telefono").doesNotExist());

        assertThat(consultarMascota.ultimoMascotaId()).isEqualTo(7L);
    }

    @Test
    void consultaRespondeNotFoundCuandoLaMascotaNoExiste() throws Exception {
        consultarMascota.fallarCon(new MascotaNoEncontradaException("Mascota 999 no encontrada"));

        mockMvc.perform(get("/mascotas/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Mascota 999 no encontrada"));
    }

    @Test
    void registroRespondeCreatedConLaMascotaYSuPropietario() throws Exception {
        registrarMascota.responderCon(REX);

        mockMvc.perform(post("/propietarios/1/mascotas").contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nombre").value("Rex"))
                .andExpect(jsonPath("$.propietario.nombre").value("Ana Perez"));

        assertThat(registrarMascota.ultimoComando()).isEqualTo(
                new RegistrarMascotaComando(1L, "Rex", "Perro", "Labrador", Sexo.M, NACIMIENTO_DE_REX));
    }

    @Test
    void registroAceptaUnaMascotaSoloConNombreYEspecie() throws Exception {
        registrarMascota.responderCon(REX);

        mockMvc.perform(post("/propietarios/1/mascotas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Rex", "especie": "Perro"}
                                """))
                .andExpect(status().isCreated());

        assertThat(registrarMascota.ultimoComando())
                .isEqualTo(new RegistrarMascotaComando(1L, "Rex", "Perro", null, null, null));
    }

    @Test
    void registroRespondeNotFoundCuandoElPropietarioNoExiste() throws Exception {
        registrarMascota.fallarCon(new PropietarioNoRegistradoException("Propietario 1 no registrado"));

        mockMvc.perform(post("/propietarios/1/mascotas").contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Propietario 1 no registrado"));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void registroRespondeBadRequestSinInvocarElCasoDeUsoCuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/propietarios/1/mascotas").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        assertThat(registrarMascota.ultimoComando()).isNull();
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                """
                {"especie": "Perro"}
                """,
                """
                {"nombre": "Rex", "especie": "   "}
                """,
                """
                {"nombre": "Rex", "especie": "Perro", "sexo": "X"}
                """,
                """
                {"nombre": "Rex", "especie": "Perro", "fechaNacimiento": "ayer"}
                """,
                "esto no es json");
    }

    @Test
    void respondeServiceUnavailableSinFiltrarElDetalleTecnico() throws Exception {
        consultarMascota.fallarCon(new AccesoADatosException("No se pudo consultar la mascota",
                new IllegalStateException("password=secreta")));

        mockMvc.perform(get("/mascotas/7"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(content().string(not(containsString("secreta"))));
    }

    @Test
    void respondeInternalServerErrorSinStacktraceAnteUnErrorInesperado() throws Exception {
        consultarMascota.fallarCon(new IllegalStateException("detalle interno"));

        mockMvc.perform(get("/mascotas/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("detalle interno"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
