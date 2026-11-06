package com.vetsync.appointment.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Contrato de {@link ClinicaRestAdapter} frente a {@code GET /mascotas/{id}} de clinic-service,
 * con un servidor simulado (sin red ni Docker).
 */
class ClinicaRestAdapterTest {

    private static final String URL_DE_REX = "http://clinic-service/mascotas/7";
    private static final String RESPUESTA_DE_REX = """
            {"id": 7, "nombre": "Rex", "propietario": {"id": 1, "nombre": "Ana Perez", "correo": "ana@correo.com"}}
            """;

    private MockRestServiceServer servidor;
    private ClinicaGateway clinicaGateway;

    @BeforeEach
    void prepararServidorSimulado() {
        RestClient.Builder constructor = RestClient.builder().baseUrl("http://clinic-service");
        servidor = MockRestServiceServer.bindTo(constructor).build();
        clinicaGateway = new ClinicaRestAdapter(constructor.build());
    }

    @Test
    void devuelveElContactoDeLaMascotaRegistrada() {
        servidor.expect(requestTo(URL_DE_REX))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RESPUESTA_DE_REX, MediaType.APPLICATION_JSON));

        Optional<ContactoDeMascota> contacto = clinicaGateway.buscarContactoDeMascota(7L);

        assertThat(contacto).contains(new ContactoDeMascota("Rex", "Ana Perez", "ana@correo.com"));
        servidor.verify();
    }

    @Test
    void devuelveVacioCuandoLaMascotaNoExiste() {
        servidor.expect(requestTo(URL_DE_REX)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(clinicaGateway.buscarContactoDeMascota(7L)).isEmpty();
        servidor.verify();
    }

    @Test
    void lanzaAccesoADatosExceptionCuandoClinicServiceRespondeConError() {
        servidor.expect(requestTo(URL_DE_REX)).andRespond(withServerError());

        assertThatThrownBy(() -> clinicaGateway.buscarContactoDeMascota(7L))
                .isInstanceOf(AccesoADatosException.class);
    }

    @Test
    void lanzaAccesoADatosExceptionConLaCausaCuandoNoHayConexion() {
        servidor.expect(requestTo(URL_DE_REX)).andRespond(withException(new IOException("sin conexion")));

        assertThatThrownBy(() -> clinicaGateway.buscarContactoDeMascota(7L))
                .isInstanceOf(AccesoADatosException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }
}
