package com.vetsync.clinic.application.service;

import com.vetsync.clinic.application.port.in.ConsultarMascotaUseCase;
import com.vetsync.clinic.application.port.out.MascotaRepository;
import com.vetsync.clinic.domain.MascotaConPropietario;
import com.vetsync.clinic.domain.MascotaNoEncontradaException;

/** Consulta una mascota con su propietario. */
public class ConsultarMascotaService implements ConsultarMascotaUseCase {

    private final MascotaRepository mascotaRepository;

    public ConsultarMascotaService(MascotaRepository mascotaRepository) {
        this.mascotaRepository = mascotaRepository;
    }

    @Override
    public MascotaConPropietario consultarMascota(long mascotaId) {
        return mascotaRepository.buscarConPropietario(mascotaId)
                .orElseThrow(() -> new MascotaNoEncontradaException("La mascota " + mascotaId + " no existe"));
    }
}
