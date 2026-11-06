package com.vetsync.notification.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.port.out.AgendaGateway;
import com.vetsync.notification.domain.CitaPorRecordar;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Contrato de {@link AgendaRestAdapter} frente a {@code GET /citas/por-recordar} de appointment-service,
 * con un servidor simulado (sin red ni Docker).
 */
class AgendaRestAdapterTest {

    private static final LocalDateTime DESDE = LocalDateTime.of(2030, 6, 10, 10, 0);
    private static final LocalDateTime HASTA = LocalDateTime.of(2030, 6, 11, 10, 0);
    private static final String URL =
            "http://appointment-service/citas/por-recordar?desde=2030-06-10T10:00:00&hasta=2030-06-11T10:00:00";
    private static final String RESPUESTA = """
            [{"id": 5, "inicio": "2030-06-10T18:00:00", "mascotaNombre": "Rex", "contactoNombre": "Ana Perez",
              "contactoCorreo": "ana@correo.com", "motivo": "Control anual", "estado": "PROGRAMADA"}]
            """;

    private MockRestServiceServer servidor;
    private AgendaGateway agendaGateway;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://appointment-service");
        servidor = MockRestServiceServer.bindTo(constructor).build();
        agendaGateway = new AgendaRestAdapter(constructor.build());
    }

    @Test
    void devuelveLasCitasPorRecordarIgnorandoLosCamposQueNoUsa() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA, MediaType.APPLICATION_JSON));

        List<CitaPorRecordar> citas = agendaGateway.buscarCitasPorRecordar(DESDE, HASTA);

        assertThat(citas).containsExactly(new CitaPorRecordar(
                5L, LocalDateTime.of(2030, 6, 10, 18, 0), "Rex", "Ana Perez", "ana@correo.com", "Control anual"));
        servidor.verify();
    }

    @Test
    void devuelveUnaListaVaciaCuandoNoHayCitas() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(agendaGateway.buscarCitasPorRecordar(DESDE, HASTA)).isEmpty();
    }

    @Test
    void lanzaAccesoADatosExceptionCuandoAppointmentServiceRespondeConError() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> agendaGateway.buscarCitasPorRecordar(DESDE, HASTA))
                .isInstanceOf(AccesoADatosException.class);
    }

    @Test
    void lanzaAccesoADatosExceptionCuandoLaRespuestaVieneVacia() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> agendaGateway.buscarCitasPorRecordar(DESDE, HASTA))
                .isInstanceOf(AccesoADatosException.class);
    }

    @Test
    void lanzaAccesoADatosExceptionConLaCausaCuandoNoHayConexion() {
        servidor.expect(requestTo(URL)).andRespond(withException(new IOException("sin conexion")));

        assertThatThrownBy(() -> agendaGateway.buscarCitasPorRecordar(DESDE, HASTA))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }
}
