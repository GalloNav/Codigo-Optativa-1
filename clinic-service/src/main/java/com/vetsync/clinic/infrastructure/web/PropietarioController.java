package com.vetsync.clinic.infrastructure.web;

import com.vetsync.clinic.application.port.in.RegistrarPropietarioUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador de entrada HTTP para propietarios. */
@RestController
@RequestMapping("/propietarios")
public class PropietarioController {

    private final RegistrarPropietarioUseCase registrarPropietario;

    public PropietarioController(RegistrarPropietarioUseCase registrarPropietario) {
        this.registrarPropietario = registrarPropietario;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropietarioResponse registrar(@RequestBody RegistrarPropietarioRequest solicitud) {
        return PropietarioResponse.desde(registrarPropietario.registrarPropietario(solicitud.aComando()));
    }
}
