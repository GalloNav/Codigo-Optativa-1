package com.vetsync.appointment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsync.appointment.application.fakes.FakeListarVeterinariosUseCase;
import com.vetsync.appointment.application.fakes.FakeRegistrarVeterinarioUseCase;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioComando;
import com.vetsync.appointment.domain.Veterinario;
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

/** Contrato HTTP de {@code /veterinarios}. Solo la capa web: los casos de uso son fakes. */
@WebMvcTest(VeterinarioController.class)
class VeterinarioControllerTest {

    private static final Veterinario ANA = new Veterinario(1L, "Ana Gomez", "Cirugia");
    private static final Veterinario LUIS = new Veterinario(2L, "Luis Mora", null);

    /** Registra los fakes como los beans de los puertos de entrada que necesita el controller. */
    @TestConfiguration
    static class Escenario {

        @Bean
        FakeRegistrarVeterinarioUseCase registrarVeterinarioUseCase() {
            return new FakeRegistrarVeterinarioUseCase();
        }

        @Bean
        FakeListarVeterinariosUseCase listarVeterinariosUseCase() {
            return new FakeListarVeterinariosUseCase();
        }
    }

    private final MockMvc mockMvc;
    private final FakeRegistrarVeterinarioUseCase registrarVeterinario;
    private final FakeListarVeterinariosUseCase listarVeterinarios;

    @Autowired
    VeterinarioControllerTest(
            MockMvc mockMvc,
            FakeRegistrarVeterinarioUseCase registrarVeterinario,
            FakeListarVeterinariosUseCase listarVeterinarios) {
        this.mockMvc = mockMvc;
        this.registrarVeterinario = registrarVeterinario;
        this.listarVeterinarios = listarVeterinarios;
    }

    @BeforeEach
    void reiniciarLosCasosDeUso() {
        registrarVeterinario.reiniciar();
        listarVeterinarios.reiniciar();
    }

    @Test
    void registroRespondeCreatedConElVeterinarioRegistrado() throws Exception {
        registrarVeterinario.responderCon(ANA);

        mockMvc.perform(post("/veterinarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Ana Gomez", "especialidad": "Cirugia"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana Gomez"))
                .andExpect(jsonPath("$.especialidad").value("Cirugia"));

        assertThat(registrarVeterinario.ultimoComando())
                .isEqualTo(new RegistrarVeterinarioComando("Ana Gomez", "Cirugia"));
    }

    @Test
    void registroAceptaUnVeterinarioSinEspecialidad() throws Exception {
        registrarVeterinario.responderCon(LUIS);

        mockMvc.perform(post("/veterinarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Luis Mora"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especialidad").value(nullValue()));

        assertThat(registrarVeterinario.ultimoComando())
                .isEqualTo(new RegistrarVeterinarioComando("Luis Mora", null));
    }

    @ParameterizedTest
    @MethodSource("cuerposInvalidos")
    void registroRespondeBadRequestSinInvocarElCasoDeUsoCuandoElCuerpoEsInvalido(String cuerpo) throws Exception {
        mockMvc.perform(post("/veterinarios").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        assertThat(registrarVeterinario.ultimoComando()).isNull();
    }

    static Stream<String> cuerposInvalidos() {
        return Stream.of(
                "{\"especialidad\": \"Cirugia\"}",
                "{\"nombre\": \"   \"}",
                "{\"nombre\": \"" + "x".repeat(101) + "\"}",
                "{\"nombre\": \"Ana Gomez\", \"especialidad\": \"   \"}",
                "{\"nombre\": \"Ana Gomez\", \"especialidad\": \"" + "x".repeat(61) + "\"}",
                "esto no es json");
    }

    @Test
    void listadoDevuelveLosVeterinariosEnElOrdenRecibido() throws Exception {
        listarVeterinarios.responderCon(List.of(ANA, LUIS));

        mockMvc.perform(get("/veterinarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ana Gomez"))
                .andExpect(jsonPath("$[0].especialidad").value("Cirugia"))
                .andExpect(jsonPath("$[1].nombre").value("Luis Mora"))
                .andExpect(jsonPath("$[1].especialidad").value(nullValue()));
    }

    @Test
    void listadoDevuelveUnArregloVacioCuandoNoHayVeterinarios() throws Exception {
        mockMvc.perform(get("/veterinarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
