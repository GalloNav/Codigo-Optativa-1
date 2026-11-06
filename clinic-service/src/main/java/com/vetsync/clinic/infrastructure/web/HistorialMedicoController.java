package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.ConsultarHistorialMedicoUseCase;
import com.vetsync.clinic.application.port.in.RegistrarEntradaMedicaUseCase;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP para el historial médico de una mascota. */
@RestController
@RequestMapping("/mascotas/{mascotaId}/historial")
public class HistorialMedicoController {

    private final RegistrarEntradaMedicaUseCase registrarEntradaMedica;
    private final ConsultarHistorialMedicoUseCase consultarHistorialMedico;

    public HistorialMedicoController(
            RegistrarEntradaMedicaUseCase registrarEntradaMedica,
            ConsultarHistorialMedicoUseCase consultarHistorialMedico) {
        this.registrarEntradaMedica = registrarEntradaMedica;
        this.consultarHistorialMedico = consultarHistorialMedico;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroMedicoResponse registrar(
            @PathVariable long mascotaId, @RequestBody RegistrarEntradaMedicaRequest solicitud) {
        return RegistroMedicoResponse.desde(
                registrarEntradaMedica.registrarEntradaMedica(solicitud.aComando(mascotaId)));
    }

    @GetMapping
    public List<RegistroMedicoResponse> consultar(@PathVariable long mascotaId) {
        return consultarHistorialMedico.consultarHistorial(mascotaId).stream()
                .map(RegistroMedicoResponse::desde)
                .toList();
    }
}
