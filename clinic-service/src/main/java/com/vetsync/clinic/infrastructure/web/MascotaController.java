package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.ConsultarMascotaUseCase;
import com.vetsync.clinic.application.port.in.RegistrarMascotaUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP para mascotas. */
@RestController
public class MascotaController {

    private final RegistrarMascotaUseCase registrarMascota;
    private final ConsultarMascotaUseCase consultarMascota;

    public MascotaController(RegistrarMascotaUseCase registrarMascota, ConsultarMascotaUseCase consultarMascota) {
        this.registrarMascota = registrarMascota;
        this.consultarMascota = consultarMascota;
    }

    @PostMapping("/propietarios/{propietarioId}/mascotas")
    @ResponseStatus(HttpStatus.CREATED)
    public MascotaResponse registrar(
            @PathVariable long propietarioId, @RequestBody RegistrarMascotaRequest solicitud) {
        return MascotaResponse.desde(registrarMascota.registrarMascota(solicitud.aComando(propietarioId)));
    }

    @GetMapping("/mascotas/{mascotaId}")
    public MascotaResponse consultar(@PathVariable long mascotaId) {
        return MascotaResponse.desde(consultarMascota.consultarMascota(mascotaId));
    }
}
