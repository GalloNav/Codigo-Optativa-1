package com.vetsync.clinic.application.service;

import com.vetsync.clinic.application.port.in.RegistrarPropietarioComando;
import com.vetsync.clinic.application.port.in.RegistrarPropietarioUseCase;
import com.vetsync.clinic.application.port.out.PropietarioRepository;
import com.vetsync.clinic.domain.Propietario;

/** Registra propietarios. */
public class RegistrarPropietarioService implements RegistrarPropietarioUseCase {

    private final PropietarioRepository propietarioRepository;

    public RegistrarPropietarioService(PropietarioRepository propietarioRepository) {
        this.propietarioRepository = propietarioRepository;
    }

    @Override
    public Propietario registrarPropietario(RegistrarPropietarioComando comando) {
        return propietarioRepository.guardar(
                new Propietario(null, comando.nombre(), comando.correo(), comando.telefono()));
    }
}
