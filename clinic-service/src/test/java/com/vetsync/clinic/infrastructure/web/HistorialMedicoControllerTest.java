package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.clinic.application.fakes.FakeConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.application.fakes.FakeRegistrarEntradaMedicaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaComando;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;
import com.vetsync.clinic.domain.RegistroMedico;
import com.vetsync.clinic.domain.TipoRegistro;
import java.math.BigDecimal;
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

/** Contrato HTTP del historial médico. Solo la capa web: los casos de uso son fakes. */
@WebMvcTest(HistorialMedicoController.class)
class HistorialMedicoControllerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final RegistroMedico CONSULTA = new RegistroMedico(1L, 7L, FECHA, TipoRegistro.CONSULTA,
            "Control anual", "Sano", "Ninguno", new BigDecimal("12.50"), 3L);
    private static final RegistroMedico VACUNA = new RegistroMedico(2L, 7L, FECHA.plusDays(1),
            TipoRegistro.VACUNA, "Vacuna anual", null, null, null, null);
    private static final String CUERPO_COMPLETO = """
            {"tipo": "CONSULTA", "motivo": "Control anual", "diagnostico": "Sano", "tratamiento": "Ninguno",
             "pesoKg": 12.5, "veterinarioId": 3}
            """;

    /** Registra los fakes como los beans de los puertos de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeRegistrarEntradaMedicaUseCase registrarEntradaMedicaUseCase() {
            return new FakeRegistrarEntradaMedicaUseCase();
        }

        @Bean
        FakeConsultarHistorialMedicoUseCase consultarHistorialMedicoUseCase() {
            return new FakeConsultarHistorialMedicoUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeRegistrarEntradaMedicaUseCase registrarEntradaMedica;
    private final FakeConsultarHistorialMedicoUseCase consultarHistorial;

    @Autowired
    HistorialMedicoControllerTest(
            MockMvc mockMvc,
            FakeRegistrarEntradaMedicaUseCase registrarEntradaMedica,
            FakeConsultarHistorialMedicoUseCase consultarHistorial) {
        this.mockMvc = mockMvc;
        this.registrarEntradaMedica = registrarEntradaMedica;
        this.consultarHistorial = consultarHistorial;
    }

    @BeforeEach
    void reiniciarLosCasosDeUso() {
        registrarEntradaMedica.reiniciar();
        consultarHistorial.reiniciar();
    }

    @Test
    void registroRespondeCreatedConLaEntradaGuardada() throws Exception {
        registrarEntradaMedica.responderCon(CONSULTA);

        mockMvc.perform(post("/mascotas/7/historial").contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPLETO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fecha").value("2030-06-10T10:00:00"))
                .andExpect(jsonPath("$.tipo").value("CONSULTA"))
                .andExpect(jsonPath("$.motivo").value("Control anual"))
                .andExpect(jsonPath("$.diagnostico").value("Sano"))
                .andExpect(jsonPath("$.tratamiento").value("Ninguno"))
                .andExpect(jsonPath("$.pesoKg").value(12.5))
                .andExpect(jsonPath("$.veterinarioId").value(3));

        assertThat(registrarEntradaMedica.ultimoComando()).isEqualTo(new RegistrarEntradaMedicaComando(
                7L, TipoRegistro.CONSULTA, "Control anual", "Sano", "Ninguno", new BigDecimal("12.5"), 3L));
    }

    @Test
    void registroAceptaUnaEntradaSoloConTipoYMotivo() throws Exception {
        registrarEntradaMedica.responderCon(VACUNA);

        mockMvc.perform(post("/mascotas/7/historial").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "VACUNA", "motivo": "Vacuna anual"}
                                """))
                .andExpect(status().isCreated());

        assertThat(registrarEntradaMedica.ultimoComando()).isEqualTo(new RegistrarEntradaMedicaComando(
                7L, TipoRegistro.VACUNA, "Vacuna anual", null, null, null, null));
    }

    @Test
    void registroRespondeNotFoundCuandoLaMascotaNoExiste() throws Exception {
        registrarEntradaMedica.fallarCon(new MascotaNoEncontradaException("La mascota 7 no existe"));

        mockMvc.perform(post("/mascotas/7/historial").contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPLETO))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("La mascota 7 no existe"));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void registroRespondeBadRequestSinInvocarElCasoDeUsoCuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/mascotas/7/historial").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        assertThat(registrarEntradaMedica.ultimoComando()).isNull();
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                "{\"motivo\": \"Control\"}",
                "{\"tipo\": \"CONSULTA\"}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"   \"}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"" + "x".repeat(201) + "\"}",
                "{\"tipo\": \"OPERACION\", \"motivo\": \"Control\"}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"Control\", \"pesoKg\": 0}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"Control\", \"pesoKg\": -3}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"Control\", \"pesoKg\": 1000}",
                "{\"tipo\": \"CONSULTA\", \"motivo\": \"Control\", \"veterinarioId\": 0}",
                "esto no es json");
    }

    @Test
    void consultaDevuelveElHistorialEnElOrdenRecibido() throws Exception {
        consultarHistorial.responderCon(List.of(VACUNA, CONSULTA));

        mockMvc.perform(get("/mascotas/7/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].tipo").value("VACUNA"))
                .andExpect(jsonPath("$[0].diagnostico").value(nullValue()))
                .andExpect(jsonPath("$[0].pesoKg").value(nullValue()))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].pesoKg").value(12.5));

        assertThat(consultarHistorial.ultimoMascotaId()).isEqualTo(7L);
    }

    @Test
    void consultaDevuelveUnArregloVacioCuandoNoHayEntradas() throws Exception {
        mockMvc.perform(get("/mascotas/7/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void consultaRespondeNotFoundCuandoLaMascotaNoExiste() throws Exception {
        consultarHistorial.fallarCon(new MascotaNoEncontradaException("La mascota 7 no existe"));

        mockMvc.perform(get("/mascotas/7/historial"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La mascota 7 no existe"));
    }
}
