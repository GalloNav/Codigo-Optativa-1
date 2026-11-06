package com.vetsync.appointment.infrastructure.client;

import com.vetsync.appointment.application.port.out.AccesoADatosException;
import com.vetsync.appointment.application.port.out.ClinicaGateway;
import com.vetsync.appointment.application.port.out.ContactoDeMascota;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador de {@link ClinicaGateway} que consulta {@code GET /mascotas/{id}} en clinic-service.
 * Un 404 significa "mascota no registrada"; cualquier otro fallo es un error técnico.
 */
public class ClinicaRestAdapter implements ClinicaGateway {

    private final RestClient restClient;

    public ClinicaRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ContactoDeMascota> buscarContactoDeMascota(long mascotaId) {
        try {
            return restClient.get()
                    .uri("/mascotas/{id}", mascotaId)
                    .exchange((solicitud, respuesta) -> interpretar(mascotaId, respuesta));
        } catch (RestClientException causa) {
            throw new AccesoADatosException(
                    "No se pudo consultar la mascota " + mascotaId + " en clinic-service", causa);
        }
    }

    private static Optional<ContactoDeMascota> interpretar(long mascotaId, ConvertibleClientHttpResponse respuesta)
            throws IOException {
        HttpStatusCode estado = respuesta.getStatusCode();
        if (estado.value() == HttpStatus.NOT_FOUND.value()) {
            return Optional.empty();
        }
        if (estado.isError()) {
            throw new AccesoADatosException(
                    "clinic-service respondio " + estado.value() + " al consultar la mascota " + mascotaId);
        }
        MascotaDto mascota = respuesta.bodyTo(MascotaDto.class);
        if (mascota == null || mascota.propietario() == null) {
            throw new AccesoADatosException(
                    "clinic-service devolvio una respuesta incompleta para la mascota " + mascotaId);
        }
        return Optional.of(new ContactoDeMascota(
                mascota.nombre(), mascota.propietario().nombre(), mascota.propietario().correo()));
    }

    /** Forma de la respuesta de clinic-service; los campos que no se usan se ignoran. */
    record MascotaDto(String nombre, PropietarioDto propietario) {
    }

    record PropietarioDto(String nombre, String correo) {
    }
}
