package com.vetsync.appointment.application.service;

import com.vetsync.appointment.application.port.in.RegistrarVeterinarioComando;
import com.vetsync.appointment.application.port.in.RegistrarVeterinarioUseCase;
import com.vetsync.appointment.application.port.out.VeterinarioRepository;
import com.vetsync.appointment.domain.Veterinario;

/** Registra veterinarios. */
public class RegistrarVeterinarioService implements RegistrarVeterinarioUseCase {

    private final VeterinarioRepository veterinarioRepository;

    public RegistrarVeterinarioService(VeterinarioRepository veterinarioRepository) {
        this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    public Veterinario registrarVeterinario(RegistrarVeterinarioComando comando) {
        return veterinarioRepository.guardar(new Veterinario(null, comando.nombre(), comando.especialidad()));
    }
}
