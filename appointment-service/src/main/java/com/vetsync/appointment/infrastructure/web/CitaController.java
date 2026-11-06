package com.vetsync.appointment.infrastructure.web;

import com.vetsync.appointment.application.port.in.AgendarCitaUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP del caso de uso de agendar citas. */
@RestController
@RequestMapping("/citas")
public class CitaController {

    private final AgendarCitaUseCase agendarCita;

    public CitaController(AgendarCitaUseCase agendarCita) {
        this.agendarCita = agendarCita;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponse agendar(@RequestBody AgendarCitaRequest solicitud) {
        return CitaResponse.desde(agendarCita.agendarCita(solicitud.aComando()));
    }
}
