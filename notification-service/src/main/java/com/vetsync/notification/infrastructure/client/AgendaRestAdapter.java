package com.vetsync.notification.infrastructure.client;

import com.vetsync.notification.application.port.out.AccesoADatosException;
import com.vetsync.notification.application.port.out.AgendaGateway;
import com.vetsync.notification.domain.CitaPorRecordar;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador de {@link AgendaGateway} que consulta {@code GET /citas/por-recordar} en appointment-service.
 * Cualquier fallo (red, código de error o respuesta vacía) es un error técnico.
 */
public class AgendaRestAdapter implements AgendaGateway {

    private final RestClient restClient;

    public AgendaRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<CitaPorRecordar> buscarCitasPorRecordar(LocalDateTime desde, LocalDateTime hasta) {
        try {
            return restClient.get()
                    .uri(constructor -> constructor.path("/citas/por-recordar")
                            .queryParam("desde", formatear(desde))
                            .queryParam("hasta", formatear(hasta))
                            .build())
                    .exchange((solicitud, respuesta) -> interpretar(respuesta));
        } catch (RestClientException causa) {
            throw new AccesoADatosException(
                    "No se pudieron consultar las citas por recordar en appointment-service", causa);
        }
    }

    private static String formatear(LocalDateTime momento) {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(momento.truncatedTo(ChronoUnit.SECONDS));
    }

    private static List<CitaPorRecordar> interpretar(ConvertibleClientHttpResponse respuesta) throws IOException {
        HttpStatusCode estado = respuesta.getStatusCode();
        if (estado.isError()) {
            throw new AccesoADatosException(
                    "appointment-service respondio " + estado.value() + " al consultar las citas por recordar");
        }
        CitaDto[] citas = respuesta.bodyTo(CitaDto[].class);
        if (citas == null) {
            throw new AccesoADatosException(
                    "appointment-service devolvio una respuesta vacia al consultar las citas por recordar");
        }
        return Arrays.stream(citas).map(CitaDto::aDominio).toList();
    }

    /** Forma de la respuesta de appointment-service; los campos que no se usan se ignoran. */
    record CitaDto(
            long id,
            LocalDateTime inicio,
            String mascotaNombre,
            String contactoNombre,
            String contactoCorreo,
            String motivo) {

        CitaPorRecordar aDominio() {
            return new CitaPorRecordar(id, inicio, mascotaNombre, contactoNombre, contactoCorreo, motivo);
        }
    }
}
