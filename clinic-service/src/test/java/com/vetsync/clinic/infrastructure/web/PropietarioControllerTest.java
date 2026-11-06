package com.vetsync.clinic.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.clinic.application.fakes.FakeRegistrarPropietarioUseCase;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioComando;
import com.vetsync.clinic.domain.CorreoYaRegistradoException;
import com.vetsync.clinic.domain.Propietario;
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

/** Contrato HTTP de {@code POST /propietarios}. Solo la capa web: el caso de uso es un fake. */
@WebMvcTest(PropietarioController.class)
class PropietarioControllerTest {

    private static final String CUERPO_VALIDO = """
            {"nombre": "Ana Perez", "correo": "ana@correo.com", "telefono": "555-1234"}
            """;

    /** Registra el fake como el bean del puerto de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeRegistrarPropietarioUseCase registrarPropietarioUseCase() {
            return new FakeRegistrarPropietarioUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeRegistrarPropietarioUseCase casoDeUso;

    @Autowired
    PropietarioControllerTest(MockMvc mockMvc, FakeRegistrarPropietarioUseCase casoDeUso) {
        this.mockMvc = mockMvc;
        this.casoDeUso = casoDeUso;
    }

    @BeforeEach
    void reiniciarElCasoDeUso() {
        casoDeUso.reiniciar();
    }

    @Test
    void respondeCreatedConElPropietarioRegistrado() throws Exception {
        casoDeUso.responderCon(new Propietario(1L, "Ana Perez", "ana@correo.com", "555-1234"));

        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana Perez"))
                .andExpect(jsonPath("$.correo").value("ana@correo.com"))
                .andExpect(jsonPath("$.telefono").value("555-1234"));

        assertThat(casoDeUso.ultimoComando())
                .isEqualTo(new RegistrarPropietarioComando("Ana Perez", "ana@correo.com", "555-1234"));
    }

    @Test
    void aceptaUnPropietarioSinTelefono() throws Exception {
        casoDeUso.responderCon(new Propietario(1L, "Ana Perez", "ana@correo.com", null));

        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana Perez", "correo": "ana@correo.com"}
                                """))
                .andExpect(status().isCreated());

        assertThat(casoDeUso.ultimoComando())
                .isEqualTo(new RegistrarPropietarioComando("Ana Perez", "ana@correo.com", null));
    }

    @Test
    void respondeConflictCuandoElCorreoYaEstaRegistrado() throws Exception {
        casoDeUso.fallarCon(new CorreoYaRegistradoException("El correo ana@correo.com ya esta registrado"));

        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("El correo ana@correo.com ya esta registrado"));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void respondeBadRequestSinInvocarElCasoDeUsoCuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/propietarios").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        assertThat(casoDeUso.ultimoComando()).isNull();
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                """
                {"nombre": "Ana Perez"}
                """,
                """
                {"nombre": "   ", "correo": "ana@correo.com"}
                """,
                """
                {"nombre": "Ana Perez", "correo": "no-es-un-correo"}
                """,
                "esto no es json");
    }
}
